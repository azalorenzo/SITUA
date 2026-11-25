package com.progweb.backend.servicios;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import com.progweb.backend.errores.AccesoRechazado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ClienteDaily implements ProveedorLlamadas {
    private static final org.slf4j.Logger registro=org.slf4j.LoggerFactory.getLogger(ClienteDaily.class);
    private final HttpClient cliente=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json;
    private final String clave;
    private final boolean activo;

    public ClienteDaily(ObjectMapper json,@Value("${llamadas.clave:}") String clave,
            @Value("${llamadas.habilitadas:false}") boolean activo) {
        this.json=json;this.clave=clave.strip();this.activo=activo;
        if(activo&&this.clave.isBlank())throw new IllegalArgumentException("Completa la clave de Daily.");
    }
    public boolean habilitado(){return activo&&!clave.isBlank();}

    private JsonNode enviar(String ruta,String metodo,Object cuerpo) {
        if(!habilitado())throw new AccesoRechazado(503,"Las llamadas no están disponibles.");
        var peticion=HttpRequest.newBuilder(URI.create("https://api.daily.co/v1/"+ruta))
            .timeout(Duration.ofSeconds(8)).header("Authorization","Bearer "+clave).header("Content-Type","application/json")
            .method(metodo,cuerpo==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo))).build();
        try {
            var respuesta=cliente.send(peticion,HttpResponse.BodyHandlers.ofString());
            if(respuesta.statusCode()==404&&ruta.startsWith("rooms/"))return json.readTree("{}");
            if(respuesta.statusCode()<200||respuesta.statusCode()>=300){registro.warn("Daily respondió con estado HTTP {}.",respuesta.statusCode());throw new AccesoRechazado(502,"No se pudo conectar la llamada. Intenta nuevamente.");}
            return json.readTree(respuesta.body());
        } catch(InterruptedException fallo){Thread.currentThread().interrupt();throw new AccesoRechazado(502,"Se interrumpió la conexión de la llamada.");}
        catch(AccesoRechazado fallo){throw fallo;}
        catch(Exception fallo){registro.warn("Falló la conexión con Daily: {}.",fallo.getClass().getSimpleName());throw new AccesoRechazado(502,"No se pudo conectar la llamada. Intenta nuevamente.");}
    }
    private Map<String,Object> permisos(String tipo) {
        return Map.of("canSend",tipo.equals("audio")?List.of("audio"):List.of("audio","video"),"canAdmin",false);
    }
    public Sala crearSala(String nombre,String tipo,Instant vencimiento) {
        var propiedades=new HashMap<String,Object>();
        propiedades.put("exp",vencimiento.getEpochSecond());propiedades.put("eject_at_room_exp",true);
        propiedades.put("max_participants",2);propiedades.put("enforce_unique_user_ids",true);
        propiedades.put("start_video_off",tipo.equals("audio"));propiedades.put("enable_screenshare",false);
        propiedades.put("enable_chat",false);propiedades.put("enable_knocking",false);propiedades.put("lang","es");
        propiedades.put("permissions",permisos(tipo));
        var respuesta=enviar("rooms","POST",Map.of("name",nombre,"privacy","private","properties",propiedades));
        String direccion=respuesta.path("url").asString("");
        URI uri;
        try{uri=URI.create(direccion);}catch(Exception fallo){throw new AccesoRechazado(502,"No se pudo preparar la llamada.");}
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||!uri.getHost().endsWith(".daily.co")||uri.getUserInfo()!=null||!respuesta.path("name").asString("").equals(nombre))
            throw new AccesoRechazado(502,"No se pudo preparar la llamada.");
        return new Sala(nombre,direccion);
    }
    public String crearAcceso(String sala,String tipo,long usuario,String nombre,Instant vencimiento) {
        var propiedades=new HashMap<String,Object>();
        propiedades.put("room_name",sala);propiedades.put("exp",vencimiento.getEpochSecond());propiedades.put("eject_at_token_exp",true);
        propiedades.put("user_id",Long.toString(usuario));propiedades.put("user_name",nombre);propiedades.put("is_owner",false);
        propiedades.put("start_video_off",tipo.equals("audio"));propiedades.put("enable_screenshare",false);
        propiedades.put("enable_recording_ui",false);propiedades.put("enable_prejoin_ui",false);propiedades.put("permissions",permisos(tipo));
        String token=enviar("meeting-tokens","POST",Map.of("properties",propiedades)).path("token").asString("");
        if(token.isBlank())throw new AccesoRechazado(502,"No se pudo entrar a la llamada.");
        return token;
    }
    public boolean cerrarSala(String sala,long iniciador,long receptor) {
        if(sala==null||!sala.matches("situa-[a-f0-9-]{36}"))return true;
        boolean expirado=false,expulsado=false,eliminado=false;
        try{enviar("rooms/"+sala,"POST",Map.of("properties",Map.of("exp",Instant.now().getEpochSecond(),"eject_at_room_exp",true)));expirado=true;}catch(AccesoRechazado fallo){}
        try{enviar("rooms/"+sala+"/eject","POST",Map.of("user_ids",List.of(Long.toString(iniciador),Long.toString(receptor)),"ban",true));expulsado=true;}catch(AccesoRechazado fallo){}
        try{enviar("rooms/"+sala,"DELETE",null);eliminado=true;}catch(AccesoRechazado fallo){}
        return eliminado&&(expirado||expulsado);
    }
}
