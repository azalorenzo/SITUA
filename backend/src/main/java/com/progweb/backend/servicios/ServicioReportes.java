package com.progweb.backend.servicios;

import java.util.List;
import com.progweb.backend.errores.*;
import com.progweb.backend.solicitudes.FiltrosReportes;
import com.progweb.backend.repositorios.RepositorioReportes;
import com.progweb.backend.respuestas.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ServicioReportes {
    private final RepositorioReportes reportes;
    private final ServicioPdfReportes generador;
    private final ServicioAutenticacion autenticacion;
    public ServicioReportes(RepositorioReportes reportes,ServicioPdfReportes generador,ServicioAutenticacion autenticacion){this.reportes=reportes;this.generador=generador;this.autenticacion=autenticacion;}
    private void validar(FiltrosReportes filtros){
        if(filtros.desde()!=null&&filtros.hasta()!=null&&filtros.desde().isAfter(filtros.hasta()))throw new SolicitudRechazada(422,"La fecha final debe ser igual o posterior a la inicial.","hasta");
    }
    private void verificarDescarga(String identificador){
        var usuario=autenticacion.consultarSesion(identificador);
        if(!usuario.roles().contains("coordinador"))throw new AccesoRechazado(403,"La descarga requiere acceso de coordinación.");
    }
    public PaginaReportes listar(FiltrosReportes filtros){
        validar(filtros);var resumen=reportes.resumen(filtros);
        int paginas=Math.toIntExact((resumen.total()+filtros.tamano()-1)/filtros.tamano());
        int actual=paginas==0?1:Math.min(filtros.pagina(),paginas);
        return new PaginaReportes(reportes.listar(filtros,actual),actual,filtros.tamano(),resumen.total(),paginas,resumen,reportes.motivos(filtros,resumen.total()));
    }
    public List<RespuestaMotivoReporte> motivos(){return reportes.catalogoMotivos();}
    public byte[] exportar(FiltrosReportes filtros,String identificador){
        verificarDescarga(identificador);validar(filtros);
        return generador.generar(reportes.exportar(filtros),filtros);
    }
    public byte[] individual(long id,String identificador){
        verificarDescarga(identificador);
        var reporte=reportes.buscar(id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el reporte."));
        return generador.generar(List.of(reporte),null);
    }
}
