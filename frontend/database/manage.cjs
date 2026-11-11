// Herramienta de administración local; no se importa desde el frontend.
const fs = require('node:fs');
const path = require('node:path');
const mysql = require('../.mysql-tools/node_modules/mysql2/promise');
const schemaPath = path.join(__dirname, '001_tutoria.sql');
const expected = ['usuarios','roles','usuario_roles','departamentos','carreras','tutores','estudiantes','periodos_academicos','situaciones_academicas','mediciones_academicas','asignaciones_tutor','disponibilidades_tutor','sesiones','observaciones_estudiante','sesion_cambios','materiales_sesion','actividades_personales','motivos_reporte','reportes','avisos','aviso_destinatarios','conversaciones','conversacion_participantes','mensajes'];
function validateSource() {
  const sql = fs.readFileSync(schemaPath, 'utf8');
  const names = [...sql.matchAll(/CREATE TABLE (\w+)/g)].map(m => m[1]);
  if (names.length !== 24 || new Set(names).size !== 24 || expected.some(n => !names.includes(n))) throw Error('El script no coincide con las 24 tablas propuestas.');
  if (/\bDROP\s+(DATABASE|TABLE)\b/i.test(sql)) throw Error('El script contiene un borrado no permitido.');
  for (const [, referenced] of sql.matchAll(/REFERENCES (\w+) \(/g)) if (!expected.includes(referenced)) throw Error('Tabla referenciada desconocida: ' + referenced);
  console.log('OK: estructura del archivo, 24 tablas, referencias y ausencia de borrados.');
  return sql;
}
async function check(c) {
  const [tables] = await c.query("SELECT TABLE_NAME nombre, ENGINE motor, TABLE_TYPE tipo FROM information_schema.TABLES WHERE TABLE_SCHEMA='tutoria'");
  const base = tables.filter(t => t.tipo === 'BASE TABLE'), views = tables.filter(t => t.tipo === 'VIEW');
  if (base.length !== 24 || views.length !== 4 || expected.some(n => !base.find(t => t.nombre === n && t.motor === 'InnoDB'))) throw Error('La estructura instalada no coincide con el esquema esperado.');
  const [counts] = await c.query("SELECT CONSTRAINT_TYPE tipo, COUNT(*) cantidad FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='tutoria' GROUP BY CONSTRAINT_TYPE");
  const [[seed]] = await c.query('SELECT (SELECT COUNT(*) FROM tutoria.roles) roles, (SELECT COUNT(*) FROM tutoria.motivos_reporte) motivos');
  if (Number(seed.roles) !== 3 || Number(seed.motivos) !== 4) throw Error('Los catálogos no coinciden con la versión inicial.');
  console.log(JSON.stringify({base:'tutoria',tablas:base.length,vistas:views.length,restricciones:counts,catalogos:seed},null,2));
}
async function test(c) {
  const prefix = '__verificacion_' + Date.now();
  let assertions = 0;
  const insert = async (table, fields) => { const [r] = await c.query('INSERT INTO ' + table + ' SET ?', fields); return r.insertId; };
  async function rejected(label, action, codes) {
    try { await action(); } catch (e) { if (codes.includes(e.code)) { console.log('OK: '+label); assertions++; return; } throw e; }
    throw Error('Restricción no aplicada: '+label);
  }
  await c.query('USE tutoria');
  await c.beginTransaction();
  try {
    const user = async n => insert('usuarios',{nombre:prefix+n,correo:prefix+n+'@example.invalid',password_hash:'hash_ficticio_solo_prueba_rollback'});
    const u1=await user('tutor1'), u2=await user('estudiante'), u3=await user('tutor2');
    const dep=await insert('departamentos',{nombre:prefix});
    const car=await insert('carreras',{departamento_id:dep,clave:prefix,nombre:prefix});
    const t1=await insert('tutores',{usuario_id:u1,departamento_id:dep});
    const t2=await insert('tutores',{usuario_id:u3,departamento_id:dep});
    const e=await insert('estudiantes',{usuario_id:u2,matricula:prefix});
    const p=await insert('periodos_academicos',{clave:prefix,nombre:prefix,fecha_inicio:'2026-01-01',fecha_fin:'2026-12-31'});
    const a=await insert('situaciones_academicas',{estudiante_id:e,periodo_id:p,carrera_id:car,semestre:4});
    const ck=['ER_CHECK_CONSTRAINT_VIOLATED'];
    await rejected('promedio fuera de escala',()=>insert('mediciones_academicas',{situacion_id:a,medido_en:'2026-06-01 10:00:00',promedio:10.5,registrado_por:u1}),ck);
    await rejected('correo duplicado',()=>insert('usuarios',{nombre:prefix,correo:prefix+'tutor1@example.invalid',password_hash:'hash_ficticio_solo_prueba_rollback'}),['ER_DUP_ENTRY']);
    const assignment={estudiante_id:e,tutor_id:t1,periodo_id:p,asignado_por:u1,inicio_en:'2026-01-01 00:00:00',estado:'activa'};
    await insert('asignaciones_tutor',assignment);
    await rejected('segunda asignación activa del mismo periodo',()=>insert('asignaciones_tutor',{...assignment,tutor_id:t2}),['ER_DUP_ENTRY']);
    const slot={tutor_id:t1,inicio_en:'2026-06-10 10:00:00',fin_en:'2026-06-10 11:00:00',modalidad:'presencial',ubicacion:'Aula prueba'};
    await rejected('intervalo invertido',()=>insert('disponibilidades_tutor',{...slot,fin_en:'2026-06-10 09:00:00'}),ck);
    const d=await insert('disponibilidades_tutor',slot);
    const session={...slot,estudiante_id:e,periodo_id:p,disponibilidad_id:d,tema:prefix,creado_por:u1};
    const se=await insert('sesiones',session);
    await rejected('doble reserva de la misma disponibilidad',()=>insert('sesiones',session),['ER_DUP_ENTRY']);
    await rejected('sesión vinculada a disponibilidad de otro tutor',()=>insert('sesiones',{...session,tutor_id:t2,estado:'cancelada'}),['ER_NO_REFERENCED_ROW_2']);
    const [[m]]=await c.query('SELECT id FROM motivos_reporte ORDER BY id LIMIT 1');
    const report={estudiante_id:e,tutor_id:t1,periodo_id:p,sesion_id:se,motivo_id:m.id,fecha:'2026-06-10'};
    await rejected('reporte de sesión con tutor distinto',()=>insert('reportes',{...report,tutor_id:t2}),['ER_NO_REFERENCED_ROW_2']);
    await rejected('reporte completado sin autor y fecha de cierre',()=>insert('reportes',{...report,estado:'completado'}),ck);
    const chat=await insert('conversaciones',{creada_por:u1});
    await insert('conversacion_participantes',{conversacion_id:chat,usuario_id:u1});
    await rejected('mensaje de usuario ajeno al chat',()=>insert('mensajes',{conversacion_id:chat,emisor_id:u2,contenido:'prueba'}),['ER_NO_REFERENCED_ROW_2']);
    await rejected('mensaje vacío',()=>insert('mensajes',{conversacion_id:chat,emisor_id:u1,contenido:'  '}),ck);
    const notice=await insert('avisos',{autor_id:u1,titulo:prefix,descripcion:'Prueba de lectura independiente'});
    await insert('aviso_destinatarios',{aviso_id:notice,usuario_id:u1,leido_en:'2026-06-10 12:00:00'});
    await insert('aviso_destinatarios',{aviso_id:notice,usuario_id:u2});
    const [[reads]]=await c.query('SELECT COUNT(*) total, SUM(leido_en IS NULL) pendientes FROM aviso_destinatarios WHERE aviso_id=?',[notice]);
    if(Number(reads.total)!==2||Number(reads.pendientes)!==1)throw Error('Lectura por usuario incorrecta');
    assertions++; console.log('OK: lectura de avisos independiente por destinatario');
    await rejected('borrado de perfil con historial',()=>c.query('DELETE FROM estudiantes WHERE id=?',[e]),['ER_ROW_IS_REFERENCED_2']);
    console.log('OK: '+assertions+' comprobaciones de integridad; se revierte la transacción.');
  } finally { await c.rollback(); }
  const [[remains]]=await c.query('SELECT COUNT(*) n FROM usuarios WHERE correo LIKE ?',[prefix+'%']);
  if(Number(remains.n)!==0)throw Error('La reversión no eliminó los datos temporales');
  console.log('OK: sin usuarios de prueba persistidos.');
}
(async()=>{
 let c;
 try {
  const sql=validateSource();
  const mode=process.argv[2];
  if(mode==='--validate')return;
  if(!['--create','--check','--test'].includes(mode))throw Error('Uso: node database/manage.cjs --validate|--create|--check|--test');
  const config=JSON.parse(fs.readFileSync(path.join(__dirname,'connection.local.json'),'utf8'));
  c=await mysql.createConnection({host:config.host,port:config.port,user:config.user,password:config.password,connectTimeout:5000,timezone:'Z',multipleStatements:false});
  await c.query("SET SESSION time_zone = '+00:00'");
  const [[version]]=await c.query('SELECT VERSION() version, @@version_comment producto');
  console.log(JSON.stringify(version));
  const [exists]=await c.query("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='tutoria'");
  if(mode==='--create'){
   if(exists.length)throw Error('La base tutoria ya existe. No se modifica. Usa --check para inspeccionarla.');
   const statements=sql.replace(/^\s*--.*$/gm,'').split(';').map(s=>s.trim()).filter(Boolean);
   for(let i=0;i<statements.length;i++) { try { await c.query(statements[i]); } catch(e){ console.error('Fallo en sentencia '+(i+1)+'. Puede existir DDL parcial; revisar antes de reintentar.');throw e;} }
   console.log('Base tutoria creada.'); await check(c);
  } else {
   if(!exists.length)throw Error('La base tutoria todavía no existe.');
   if(mode==='--check')await check(c); else await test(c);
  }
 } catch(e){console.error((e.code?e.code+': ':'')+e.message);process.exitCode=1;}
 finally{if(c)await c.end();}
})();
