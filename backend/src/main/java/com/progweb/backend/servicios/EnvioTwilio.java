package com.progweb.backend.servicios;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EnvioTwilio implements EnvioAvisoTelefonico {
    private final HttpClient cliente=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final boolean sms;
    private final String cuenta,clave,remitenteSms,plantillaSms;
    private final ZoneId zona;
    public EnvioTwilio(ObjectMapper json,@Value("${mensajeria.sms-habilitado:false}") boolean sms,
            @Value("${mensajeria.cuenta:}") String cuenta,
            @Value("${mensajeria.clave:}") String clave,@Value("${mensajeria.remitente-sms:}") String remitenteSms,
            @Value("${mensajeria.zona:America/Mexico_City}") String zona,
            @Value("${mensajeria.plantilla-sms-prueba:}") String plantillaSms) {
        this.json=json;this.sms=sms;this.cuenta=cuenta;this.clave=clave;
        this.remitenteSms=remitenteSms;this.zona=ZoneId.of(zona);
        this.plantillaSms=plantillaSms.strip();
        if(!this.plantillaSms.isEmpty()&&!Set.of("sms_2fa","sms_appointment_reminders","sms_order_confirmation","sms_delivery_updates","sms_customer_support","sms_marketing_promotions","sms_event_notifications","sms_account_alerts","sms_feedback_surveys","sms_internal_alerts").contains(this.plantillaSms))throw new IllegalArgumentException("Selecciona una plantilla SMS de prueba válida.");
        if(sms&&(!cuenta.matches("AC[0-9a-fA-F]{32}")||clave.isBlank()))throw new IllegalArgumentException("Completa las credenciales de Twilio.");
        if(sms&&!remitenteSms.matches("\\+[1-9][0-9]{7,14}"))throw new IllegalArgumentException("Completa el remitente SMS de Twilio.");
    }
    public boolean habilitado(String canal) { return canal.equals("sms")&&sms; }
    private String limpiar(String texto,int limite) { String limpio=texto.replaceAll("\\s+"," ").strip();return limpio.substring(0,Math.min(limpio.length(),limite)); }
    public Resultado enviar(String canal,String telefono,Confirmacion confirmacion) {
        if(!habilitado(canal))return new Resultado("omitida",null,"canal_desactivado");
        if(telefono==null||!telefono.matches("\\+[1-9][0-9]{7,14}"))return new Resultado("omitida",null,"telefono_invalido");
        var fecha=confirmacion.inicio().atZone(ZoneOffset.UTC).withZoneSameInstant(zona);
        String dia=fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),hora=fecha.format(DateTimeFormatter.ofPattern("HH:mm"));
        var campos=new LinkedHashMap<String,String>();
        campos.put("To",telefono);campos.put("From",remitenteSms);
        campos.put("Body",!plantillaSms.isEmpty()?plantillaSms:"Tutoría confirmada con "+limpiar(confirmacion.tutor(),80)+" el "+dia+" a las "+hora+". "+limpiar(confirmacion.lugar(),160)+". Hora de "+zona.getId()+".");
        String cuerpo=campos.entrySet().stream().map(campo->URLEncoder.encode(campo.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(campo.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        var peticion=HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/"+cuenta+"/Messages.json"))
            .timeout(Duration.ofSeconds(10)).header("Content-Type","application/x-www-form-urlencoded")
            .header("Authorization","Basic "+Base64.getEncoder().encodeToString((cuenta+":"+clave).getBytes(StandardCharsets.UTF_8)))
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
        try {
            var respuesta=cliente.send(peticion,HttpResponse.BodyHandlers.ofString());
            if(respuesta.statusCode()>=200&&respuesta.statusCode()<300) {
                String identificador=json.readTree(respuesta.body()).path("sid").asString("");
                return identificador.matches("(SM|MM)[0-9a-fA-F]{32}")?new Resultado("aceptada",identificador,null):new Resultado("incierta",null,"respuesta_invalida");
            }
            return new Resultado(respuesta.statusCode()>=500?"incierta":"error",null,"http_"+respuesta.statusCode());
        }catch(InterruptedException fallo) { Thread.currentThread().interrupt();return new Resultado("incierta",null,"envio_interrumpido"); }
        catch(Exception fallo) { return new Resultado("incierta",null,"sin_confirmacion"); }
    }
}
