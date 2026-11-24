package com.progweb.backend.servicios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import com.progweb.backend.errores.SolicitudRechazada;

@Component
public class EnvioCodigoSmtp implements EnvioCodigoRecuperacion {
    private final JavaMailSenderImpl correo=new JavaMailSenderImpl();
    private final String remitente;
    private final boolean activo;

    public EnvioCodigoSmtp(@Value("${correo.habilitado:false}") boolean activo,
            @Value("${correo.servidor:}") String servidor,@Value("${correo.puerto:587}") int puerto,
            @Value("${correo.usuario:}") String usuario,@Value("${correo.contrasena:}") String contrasena,
            @Value("${correo.remitente:}") String remitente,@Value("${correo.ssl:false}") boolean ssl,
            @Value("${correo.autenticacion:true}") boolean autenticacion,@Value("${correo.starttls:true}") boolean starttls) {
        this.remitente=remitente;this.activo=activo;
        if(activo&&(servidor.isBlank()||autenticacion&&(usuario.isBlank()||contrasena.isBlank())||remitente.isBlank()||puerto<1||puerto>65535))
            throw new IllegalArgumentException("Completa los datos SMTP antes de habilitar el correo.");
        correo.setHost(servidor);correo.setPort(puerto);correo.setUsername(usuario);correo.setPassword(contrasena);
        correo.setDefaultEncoding("UTF-8");
        var opciones=correo.getJavaMailProperties();
        opciones.setProperty("mail.smtp.auth",String.valueOf(autenticacion));
        opciones.setProperty("mail.smtp.ssl.enable",String.valueOf(ssl));
        opciones.setProperty("mail.smtp.starttls.enable",String.valueOf(starttls&&!ssl));
        opciones.setProperty("mail.smtp.starttls.required",String.valueOf(starttls&&!ssl));
        opciones.setProperty("mail.smtp.ssl.checkserveridentity","true");
        opciones.setProperty("mail.smtp.connectiontimeout","5000");
        opciones.setProperty("mail.smtp.timeout","5000");
        opciones.setProperty("mail.smtp.writetimeout","5000");
    }
    public boolean habilitado() { return activo; }
    public void enviar(String destino,String codigo) {
        if(!activo)throw new SolicitudRechazada(503,"La recuperación por correo todavía no está disponible.","correo");
        var mensaje=new SimpleMailMessage();
        mensaje.setFrom(remitente);mensaje.setTo(destino);
        mensaje.setSubject("Recupera tu contraseña de Tutoría");
        mensaje.setText("Tu código para cambiar la contraseña es: "+codigo+
            "\n\nVence en 10 minutos y solo puede usarse una vez.\nSi no solicitaste este cambio, puedes ignorar este correo.");
        try { correo.send(mensaje); }
        catch(MailException fallo) {
            // No mostramos al usuario los datos del servidor ni sus credenciales.
            throw new SolicitudRechazada(503,"No se pudo enviar el correo. Inténtalo más tarde.","correo");
        }
    }
}
