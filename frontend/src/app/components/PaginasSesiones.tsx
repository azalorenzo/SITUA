export function PaginasSesiones({pagina,totalPaginas,alCambiar,desactivado=false}:{pagina:number;totalPaginas:number;alCambiar:(pagina:number)=>void;desactivado?:boolean}) {
  if(!totalPaginas)return null;
  const estilo='px-3 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50';
  return <nav aria-label="Paginación" className="flex flex-wrap gap-2"><button type="button" className={estilo} disabled={desactivado||pagina<=1} onClick={()=>alCambiar(pagina-1)}>Anterior</button>{Array.from({length:totalPaginas},(_,indice)=>indice+1).map(numero=><button type="button" key={numero} className={`${estilo} ${numero===pagina?'bg-[#1F6662] text-white':''}`} aria-label={`Página ${numero}`} aria-current={numero===pagina?'page':undefined} disabled={desactivado} onClick={()=>alCambiar(numero)}>{numero}</button>)}<button type="button" className={estilo} disabled={desactivado||pagina>=totalPaginas} onClick={()=>alCambiar(pagina+1)}>Siguiente</button></nav>;
}
