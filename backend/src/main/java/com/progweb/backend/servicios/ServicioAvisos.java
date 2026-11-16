package com.progweb.backend.servicios;

import java.util.List;
import java.util.LinkedHashSet;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.RepositorioAvisos;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudAviso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ServicioAvisos {
    private final RepositorioAvisos avisos;
    private final ServicioAutenticacion autenticacion;
    public ServicioAvisos(RepositorioAvisos avisos,ServicioAutenticacion autenticacion){this.avisos=avisos;this.autenticacion=autenticacion;}
    private void validarPagina(int pagina,int tamano,String busqueda){
        if(pagina<1)throw new SolicitudRechazada(422,"La página debe ser mayor que cero.","pagina");
        if(tamano<1||tamano>100)throw new SolicitudRechazada(422,"El tamaño debe estar entre 1 y 100.","tamano");
        if(busqueda.length()>160)throw new SolicitudRechazada(422,"La búsqueda no debe superar 160 caracteres.","busqueda");
    }
    public PaginaAvisos listar(int pagina,int tamano,String prioridad,String categoria,String estado,String busqueda){
        validarPagina(pagina,tamano,busqueda);
        if(prioridad!=null&&!List.of("normal","media","alta").contains(prioridad))throw new SolicitudRechazada(422,"La prioridad no es válida.","prioridad");
        if(estado!=null&&!List.of("vigente","retirado").contains(estado))throw new SolicitudRechazada(422,"El estado no es válido.","estado");
        if(categoria!=null&&categoria.length()>40)throw new SolicitudRechazada(422,"La categoría no es válida.","categoria");
        String termino=busqueda.strip();long total=avisos.contar(prioridad,categoria,estado,termino);
        int paginas=Math.toIntExact((total+tamano-1)/tamano);int actual=paginas==0?1:Math.min(pagina,paginas);
        return new PaginaAvisos(avisos.listar(prioridad,categoria,estado,termino,tamano,(actual-1L)*tamano),actual,tamano,total,paginas,avisos.resumen());
    }
    public PaginaDestinatariosAviso personas(int pagina,int tamano,String rol,String busqueda){
        validarPagina(pagina,tamano,busqueda);
        if(rol!=null&&!List.of("estudiante","tutor").contains(rol))throw new SolicitudRechazada(422,"El rol no es válido.","rol");
        String termino=busqueda.strip();long total=avisos.contarPersonas(rol,termino);
        int paginas=Math.toIntExact((total+tamano-1)/tamano);int actual=paginas==0?1:Math.min(pagina,paginas);
        return new PaginaDestinatariosAviso(avisos.personas(rol,termino,tamano,(actual-1L)*tamano),actual,tamano,total,paginas);
    }
    public DetalleAviso detalle(long id){
        var aviso=avisos.buscar(id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el aviso."));
        return new DetalleAviso(aviso,avisos.destinatarios(id));
    }
    private long verificarAutor(String id){
        var autor=autenticacion.consultarSesion(id);
        if(!autor.roles().contains("coordinador"))throw new AccesoRechazado(403,"Esta operación requiere acceso de coordinación.");
        return autor.id();
    }
    private List<Long> resolver(SolicitudAviso solicitud,Long avisoId){
        var seleccion=solicitud.usuarioIds()==null?List.<Long>of():List.copyOf(new LinkedHashSet<>(solicitud.usuarioIds()));
        if(!solicitud.destinatarios().equals("seleccion")&&!seleccion.isEmpty())throw new SolicitudRechazada(422,"Selecciona usuarios solo al usar destinatarios específicos.","usuarioIds");
        if(solicitud.destinatarios().equals("conservar")){
            if(avisoId==null)throw new SolicitudRechazada(422,"Selecciona los destinatarios del aviso nuevo.","destinatarios");
            var existentes=avisos.destinatarios(avisoId).stream().map(RespuestaDestinatarioAviso::usuarioId).toList();
            if(existentes.isEmpty())throw new SolicitudRechazada(409,"El aviso no tiene destinatarios. Selecciona un grupo o usuarios.","destinatarios");
            return existentes;
        }
        if(solicitud.destinatarios().equals("seleccion")&&seleccion.isEmpty())throw new SolicitudRechazada(422,"Selecciona al menos un destinatario.","usuarioIds");
        var resueltos=avisos.resolver(solicitud.destinatarios(),seleccion);
        if(solicitud.destinatarios().equals("seleccion")&&resueltos.size()!=seleccion.size())throw new SolicitudRechazada(409,"Algún destinatario no existe, está inactivo o no es estudiante ni tutor.","usuarioIds");
        if(resueltos.isEmpty())throw new SolicitudRechazada(409,"No hay destinatarios activos para esta selección.","destinatarios");
        return resueltos;
    }
    @Transactional
    public DetalleAviso crear(SolicitudAviso solicitud,String identificador){
        long autor=verificarAutor(identificador);var destinatarios=resolver(solicitud,null);
        long id=avisos.crear(autor,solicitud.titulo().strip(),solicitud.descripcion().strip(),solicitud.categoria().strip(),solicitud.prioridad());
        avisos.guardarDestinatarios(id,destinatarios);return detalle(id);
    }
    @Transactional
    public DetalleAviso editar(long id,SolicitudAviso solicitud,String identificador){
        verificarAutor(identificador);
        var actual=avisos.bloquear(id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el aviso."));
        if(actual.retiradoEn()!=null)throw new AccesoRechazado(409,"Un aviso retirado no se puede editar.");
        var destinatarios=resolver(solicitud,id);
        avisos.editar(id,solicitud.titulo().strip(),solicitud.descripcion().strip(),solicitud.categoria().strip(),solicitud.prioridad());
        avisos.guardarDestinatarios(id,destinatarios);return detalle(id);
    }
    @Transactional
    public RespuestaAviso retirar(long id,String identificador){
        verificarAutor(identificador);
        var actual=avisos.bloquear(id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el aviso."));
        if(actual.retiradoEn()!=null)throw new AccesoRechazado(409,"El aviso ya está retirado.");
        if(!avisos.retirar(id))throw new AccesoRechazado(409,"El aviso tiene una fecha de publicación futura y no se puede retirar todavía.");
        return avisos.buscar(id).orElseThrow();
    }
}
