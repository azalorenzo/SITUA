package com.progweb.backend.servicios;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicioAvisosTelefonicos {
    private final JdbcTemplate consultas;
    private final EnvioAvisoTelefonico envio;
    private final TransactionTemplate transaccion;
    public ServicioAvisosTelefonicos(JdbcTemplate consultas,EnvioAvisoTelefonico envio,PlatformTransactionManager gestor) {
        this.consultas=consultas;this.envio=envio;this.transaccion=new TransactionTemplate(gestor);
    }
    public void preparar(long sesion) {
        if(envio.habilitado("sms"))consultas.update("""
            INSERT IGNORE INTO notificaciones_sesion(sesion_id,canal)
            SELECT s.id,'sms' FROM sesiones s JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios u ON u.id=e.usuario_id
            WHERE s.id=? AND s.estado='confirmada' AND u.estado='activo' AND u.telefono REGEXP '^[+][1-9][0-9]{7,14}$'
            AND u.acepta_sms
            """,sesion);
    }
    private record Pendiente(long id,String canal,String telefono,EnvioAvisoTelefonico.Confirmacion datos) { }
    private Pendiente tomar() {
        return transaccion.execute(estado->{
            consultas.update("UPDATE notificaciones_sesion SET estado='incierta',codigo_error='envio_sin_cierre',actualizado_en=UTC_TIMESTAMP(6) WHERE estado='procesando' AND actualizado_en<UTC_TIMESTAMP()-INTERVAL 10 MINUTE");
            var ids=consultas.query("SELECT id FROM notificaciones_sesion WHERE estado='pendiente' AND canal='sms' AND ? ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED",
                (fila,posicion)->fila.getLong(1),envio.habilitado("sms"));
            if(ids.isEmpty())return null;
            long id=ids.getFirst();
            var datos=consultas.query("""
                SELECT n.canal,u.telefono,tutor.nombre,s.inicio_en,COALESCE(s.ubicacion,s.enlace_virtual,'') lugar
                FROM notificaciones_sesion n JOIN sesiones s ON s.id=n.sesion_id
                JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios u ON u.id=e.usuario_id
                JOIN tutores t ON t.id=s.tutor_id JOIN usuarios tutor ON tutor.id=t.usuario_id
                WHERE n.id=? AND s.estado='confirmada' AND s.inicio_en>UTC_TIMESTAMP(6) AND u.estado='activo'
                AND u.acepta_sms
                """,(fila,posicion)->new Pendiente(id,fila.getString(1),fila.getString(2),new EnvioAvisoTelefonico.Confirmacion(fila.getString(3),fila.getObject(4,LocalDateTime.class),fila.getString(5))),id);
            if(datos.isEmpty()){consultas.update("UPDATE notificaciones_sesion SET estado='omitida',codigo_error='aviso_no_vigente',actualizado_en=UTC_TIMESTAMP(6) WHERE id=?",id);return null;}
            consultas.update("UPDATE notificaciones_sesion SET estado='procesando',actualizado_en=UTC_TIMESTAMP(6) WHERE id=?",id);
            return datos.getFirst();
        });
    }
    @Scheduled(fixedDelayString="${mensajeria.intervalo-milisegundos:5000}",initialDelayString="${mensajeria.espera-inicial-milisegundos:30000}")
    public void procesar() {
        if(!envio.habilitado("sms"))return;
        var pendiente=tomar();if(pendiente==null)return;
        var resultado=envio.enviar(pendiente.canal(),pendiente.telefono(),pendiente.datos());
        transaccion.executeWithoutResult(estado->consultas.update("UPDATE notificaciones_sesion SET estado=?,identificador_proveedor=?,codigo_error=?,actualizado_en=UTC_TIMESTAMP(6) WHERE id=? AND estado='procesando'",
            resultado.estado(),resultado.identificador(),resultado.codigo(),pendiente.id()));
    }
}
