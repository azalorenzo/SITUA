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
    private final boolean sms,whatsapp;
    private final String cuenta,clave,remitenteSms,remitenteWhatsapp,plantilla,modoWhatsapp;
    private final ZoneId zona;
    public EnvioTwilio(ObjectMapper json,@Value("${mensajeria.sms-habilitado:false}") boolean sms,
            @Value("${mensajeria.whatsapp-habilitado:false}") boolean whatsapp,@Value("${mensajeria.cuenta:}") String cuenta,
            @Value("${mensajeria.clave:}") String clave,@Value("${mensajeria.remitente-sms:}") String remitenteSms,
            @Value("${mensajeria.remitente-whatsapp:}") String remitenteWhatsapp,@Value("${mensajeria.plantilla-whatsapp:}") String plantilla,
            @Value("${mensajeria.zona:America/Mexico_City}") String zona,@Value("${mensajeria.modo-whatsapp:plantilla}") String modoWhatsapp) {
        this.json=json;this.sms=sms;this.whatsapp=whatsapp;this.cuenta=cuenta;this.clave=clave;
        this.remitenteSms=remitenteSms;this.remitenteWhatsapp=remitenteWhatsapp;this.plantilla=plantilla;this.zona=ZoneId.of(zona);this.modoWhatsapp=modoWhatsapp;
        if(!Set.of("plantilla","sandbox").contains(modoWhatsapp))throw new IllegalArgumentException("Selecciona plantilla o sandbox como modo de WhatsApp.");
        if((sms||whatsapp)&&(!cuenta.matches("AC[0-9a-fA-F]{32}")||clave.isBlank()))throw new IllegalArgumentException("Completa las credenciales de Twilio.");
        if(sms&&!remitenteSms.matches("\\+[1-9][0-9]{7,14}"))throw new IllegalArgumentException("Completa el remitente SMS de Twilio.");
        if(whatsapp&&(!remitenteWhatsapp.matches("\\+[1-9][0-9]{7,14}")||modoWhatsapp.equals("plantilla")&&!plantilla.matches("HX[0-9a-fA-F]{32}")))throw new IllegalArgumentException("Completa el remitente y la plantilla aprobada de WhatsApp.");
    }
    public boolean habilitado(String canal) { return canal.equals("sms")?sms:canal.equals("whatsapp")&&whatsapp; }
    private String limpiar(String texto,int limite) { String limpio=texto.replaceAll("\\s+"," ").strip();return limpio.substring(0,Math.min(limpio.length(),limite)); }
    public Resultado enviar(String canal,String telefono,Confirmacion confirmacion) {
        if(!habilitado(canal))return new Resultado("omitida",null,"canal_desactivado");
        if(telefono==null||!telefono.matches("\\+[1-9][0-9]{7,14}"))return new Resultado("omitida",null,"telefono_invalido");
        var fecha=confirmacion.inicio().atZone(ZoneOffset.UTC).withZoneSameInstant(zona);
        String dia=fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),hora=fecha.format(DateTimeFormatter.ofPattern("HH:mm"));
        var campos=new LinkedHashMap<String,String>();
        boolean esWhatsapp=canal.equals("whatsapp");
        campos.put("To",(esWhatsapp?"whatsapp:":"")+telefono);campos.put("From",(esWhatsapp?"whatsapp:":"")+(esWhatsapp?remitenteWhatsapp:remitenteSms));
        if(esWhatsapp&&modoWhatsapp.equals("plantilla")) {
            campos.put("ContentSid",plantilla);
            campos.put("ContentVariables",json.writeValueAsString(Map.of("1",limpiar(confirmacion.tutor(),80),"2",dia,"3",hora,"4",limpiar(confirmacion.lugar(),160))));
        }else campos.put("Body","Tutoría confirmada con "+limpiar(confirmacion.tutor(),80)+" el "+dia+" a las "+hora+". "+limpiar(confirmacion.lugar(),160)+". Hora de "+zona.getId()+".");
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
