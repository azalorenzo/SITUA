package com.progweb.backend.servicios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.respuestas.RespuestaReporte;
import com.progweb.backend.solicitudes.FiltrosReportes;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ServicioPdfReportes {
    public byte[] generar(List<RespuestaReporte> reportes,FiltrosReportes filtros){
        try(var documento=new PDDocument();var salida=new ByteArrayOutputStream();
            var regular=new ClassPathResource("fuentes/Vera.ttf").getInputStream();
            var negrita=new ClassPathResource("fuentes/VeraBd.ttf").getInputStream()){
            var fuente=PDType0Font.load(documento,regular);var fuenteNegrita=PDType0Font.load(documento,negrita);
            documento.getDocumentInformation().setTitle("Reportes académicos de tutoría");
            documento.getDocumentInformation().setAuthor("Control Académico");
            try(var diseno=new Diseno(documento,fuente,fuenteNegrita)){
                diseno.nuevaPagina();
                diseno.texto("Generado: "+DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm 'UTC'").withZone(ZoneOffset.UTC).format(Instant.now()),9,false);
                diseno.texto("Reportes incluidos: "+reportes.size(),11,true);
                if(filtros!=null){
                    diseno.texto("Criterios: Estado: "+(filtros.estado()==null?"Todos":filtros.estado())
                        +" | Periodo: "+(filtros.periodoId()==null?"Todos":"#"+filtros.periodoId())
                        +" | Motivo: "+(filtros.motivoId()==null?"Todos":"#"+filtros.motivoId()),9,false);
                    diseno.texto("Búsqueda: "+(filtros.busqueda().isBlank()?"Sin búsqueda":filtros.busqueda().strip())
                        +" | Desde: "+(filtros.desde()==null?"Sin límite":filtros.desde())+" | Hasta: "+(filtros.hasta()==null?"Sin límite":filtros.hasta()),9,false);
                }
                diseno.espacio(13);
                if(reportes.isEmpty())diseno.texto("No hay reportes que coincidan con los criterios seleccionados.",11,false);
                for(var reporte:reportes){
                    diseno.contexto="";diseno.asegurar(140);diseno.contexto="Reporte #"+reporte.id();
                    diseno.texto("Reporte #"+reporte.id()+" - "+reporte.fecha().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))
                        +" - "+(reporte.estado().equals("completado")?"Completado":"Pendiente"),13,true);
                    diseno.texto("Estudiante: "+reporte.estudiante()+" | Matrícula: "+reporte.matricula(),10,false);
                    diseno.texto("Tutor: "+reporte.tutor(),10,false);
                    diseno.texto("Periodo: "+reporte.periodo(),10,false);
                    diseno.texto("Carrera: "+reporte.carrera()+" | Semestre: "+reporte.semestre(),10,false);
                    diseno.texto("Motivo: "+reporte.motivo(),10,true);
                    if(reporte.completadoEn()!=null)diseno.texto("Completado: "+reporte.completadoEn().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))+" UTC | Por: "+reporte.completadoPor(),9,false);
                    diseno.espacio(5);diseno.texto("Contenido del reporte",10,true);
                    diseno.texto(reporte.contenido()==null||reporte.contenido().isBlank()?"Sin contenido registrado.":reporte.contenido(),10,false);
                    diseno.separador();
                }
                if(diseno.caracteresSustituidos)diseno.texto("Los caracteres que no están disponibles en la fuente se representan con ?.",8,false);
                diseno.terminar();
            }
            documento.save(salida);return salida.toByteArray();
        }catch(IOException excepcion){throw new AccesoRechazado(500,"No se pudo generar el PDF. Intenta nuevamente.");}
    }

    private static class Diseno implements AutoCloseable {
        private final PDDocument documento;
        private final PDFont regular,negrita;
        private PDPageContentStream dibujo;
        private float altura;
        private String contexto="";
        private boolean caracteresSustituidos;
        private static final float MARGEN=48;
        Diseno(PDDocument documento,PDFont regular,PDFont negrita){this.documento=documento;this.regular=regular;this.negrita=negrita;}
        void nuevaPagina()throws IOException{
            if(dibujo!=null)dibujo.close();
            var pagina=new PDPage(PDRectangle.A4);documento.addPage(pagina);dibujo=new PDPageContentStream(documento,pagina);
            float alto=pagina.getMediaBox().getHeight();
            dibujo.setNonStrokingColor(new Color(28,69,67));dibujo.addRect(0,alto-88,PDRectangle.A4.getWidth(),88);dibujo.fill();
            escribir("Control Académico",MARGEN,alto-38,18,negrita,Color.WHITE);
            escribir("Reportes de tutoría"+(contexto.isEmpty()?"":" - "+contexto+" (continuación)"),MARGEN,alto-62,10,regular,Color.WHITE);
            altura=alto-117;
        }
        void asegurar(float espacio)throws IOException{if(altura-espacio<62)nuevaPagina();}
        void espacio(float espacio)throws IOException{asegurar(espacio);altura-=espacio;}
        String limpiar(String texto,PDFont fuente)throws IOException{
            var resultado=new StringBuilder();
            for(int caracter:texto.codePoints().toArray()){
                if(caracter=='\n'){resultado.append('\n');continue;}
                if(caracter=='\r')continue;if(caracter=='\t'){resultado.append("    ");continue;}
                if(Character.isISOControl(caracter)){resultado.append(' ');continue;}
                String letra=new String(Character.toChars(caracter));
                try{fuente.encode(letra);resultado.append(letra);}catch(IllegalArgumentException excepcion){resultado.append('?');caracteresSustituidos=true;}
            }
            return resultado.toString();
        }
        float ancho(String texto,PDFont fuente,float tamano)throws IOException{return fuente.getStringWidth(texto)/1000*tamano;}
        List<String> envolver(String texto,PDFont fuente,float tamano)throws IOException{
            float disponible=PDRectangle.A4.getWidth()-2*MARGEN;var lineas=new ArrayList<String>();
            for(String parrafo:limpiar(texto,fuente).split("\n",-1)){
                if(parrafo.isBlank()){lineas.add("");continue;}
                String linea="";
                for(String palabra:parrafo.strip().split("\\s+")){
                    String candidata=linea.isEmpty()?palabra:linea+" "+palabra;
                    if(ancho(candidata,fuente,tamano)<=disponible){linea=candidata;continue;}
                    if(!linea.isEmpty()){lineas.add(linea);linea="";}
                    for(int caracter:palabra.codePoints().toArray()){
                        String letra=new String(Character.toChars(caracter));
                        if(!linea.isEmpty()&&ancho(linea+letra,fuente,tamano)>disponible){lineas.add(linea);linea="";}
                        linea+=letra;
                    }
                }
                if(!linea.isEmpty())lineas.add(linea);
            }
            return lineas;
        }
        void texto(String texto,float tamano,boolean destacado)throws IOException{
            PDFont fuente=destacado?negrita:regular;
            for(String linea:envolver(texto,fuente,tamano)){
                asegurar(tamano+7);escribir(linea,MARGEN,altura,tamano,fuente,new Color(32,62,59));altura-=tamano+6;
            }
        }
        void escribir(String texto,float x,float y,float tamano,PDFont fuente,Color color)throws IOException{
            dibujo.beginText();dibujo.setFont(fuente,tamano);dibujo.setNonStrokingColor(color);dibujo.newLineAtOffset(x,y);dibujo.showText(texto);dibujo.endText();
        }
        void separador()throws IOException{
            asegurar(25);altura-=8;dibujo.setStrokingColor(new Color(218,231,226));dibujo.setLineWidth(.6f);
            dibujo.moveTo(MARGEN,altura);dibujo.lineTo(PDRectangle.A4.getWidth()-MARGEN,altura);dibujo.stroke();altura-=20;
        }
        void terminar()throws IOException{
            if(dibujo!=null){dibujo.close();dibujo=null;}
            int numero=0,total=documento.getNumberOfPages();
            for(var pagina:documento.getPages()){
                numero++;dibujo=new PDPageContentStream(documento,pagina,PDPageContentStream.AppendMode.APPEND,true,true);
                escribir("Control Académico - Exportación de consulta",MARGEN,31,8,regular,new Color(102,126,120));
                escribir("Página "+numero+" de "+total,PDRectangle.A4.getWidth()-128,31,8,regular,new Color(102,126,120));
                dibujo.close();dibujo=null;
            }
        }
        @Override public void close()throws IOException{if(dibujo!=null){dibujo.close();dibujo=null;}}
    }
}
