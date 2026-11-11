// Datos de demostración: únicamente administración de la base, sin conectar pantallas.
const fs=require('node:fs');
const path=require('node:path');
const {spawnSync}=require('node:child_process');
const mysql=require('../.mysql-tools/node_modules/mysql2/promise');
const accounts=[
 {nombre:'Coordinadora de prueba',correo:'coordinacion@demo.example',rol:'coordinador',password:'CoordDemo!2026'},
 {nombre:'Tutor de prueba',correo:'tutor@demo.example',rol:'tutor',password:'TutorDemo!2026'},
 {nombre:'Estudiante de prueba',correo:'estudiante@demo.example',rol:'estudiante',password:'AlumnoDemo!2026'},
];
function php(code,args){const result=spawnSync('C:\\xampp\\php\\php.exe',['-n','-r',code,...args],{encoding:'utf8',timeout:15000,windowsHide:true});if(result.error||result.status!==0)throw Error('No se pudo generar/verificar el hash bcrypt con PHP local.');return result.stdout.trim();}
(async()=>{
 let c;
 try{
  const hashes=accounts.map(a=>php('echo password_hash($argv[1], PASSWORD_BCRYPT, ["cost" => 12]);',[a.password]));
  hashes.forEach((hash,i)=>{if(!hash.startsWith('$2y$12$')||php('echo password_verify($argv[1], $argv[2]) ? "ok" : "error";',[accounts[i].password,hash])!=='ok')throw Error('Hash bcrypt no verificable');});
  const cfg=JSON.parse(fs.readFileSync(path.join(__dirname,'connection.local.json'),'utf8'));
  c=await mysql.createConnection({host:cfg.host,port:cfg.port,user:cfg.user,password:cfg.password,database:'tutoria',timezone:'Z',connectTimeout:5000});
  await c.query("SET SESSION time_zone = '+00:00'");
  await c.beginTransaction();
  const [existing]=await c.query('SELECT correo FROM usuarios WHERE correo IN (?)',[accounts.map(a=>a.correo)]);
  if(existing.length)throw Error('Ya existe alguna cuenta de demostración. No se reemplazan cuentas ni contraseñas.');
  const [roles]=await c.query('SELECT id,codigo FROM roles');
  for(const a of accounts)if(!roles.find(r=>r.codigo===a.rol))throw Error('Falta el rol '+a.rol);
  const [department]=await c.query('SELECT id FROM departamentos WHERE nombre=?',['Departamento de prueba']);
  let departmentId=department[0]?.id;
  if(!departmentId){const [r]=await c.query('INSERT INTO departamentos (nombre) VALUES (?)',['Departamento de prueba']);departmentId=r.insertId;}
  for(let i=0;i<accounts.length;i++){
   const a=accounts[i];
   const [r]=await c.query('INSERT INTO usuarios (nombre,correo,password_hash,estado) VALUES (?,?,?,?)',[a.nombre,a.correo,hashes[i],'activo']);
   await c.query('INSERT INTO usuario_roles (usuario_id,rol_id) VALUES (?,?)',[r.insertId,roles.find(role=>role.codigo===a.rol).id]);
   if(a.rol==='tutor')await c.query('INSERT INTO tutores (usuario_id,departamento_id) VALUES (?,?)',[r.insertId,departmentId]);
   if(a.rol==='estudiante')await c.query('INSERT INTO estudiantes (usuario_id,matricula) VALUES (?,?)',[r.insertId,'DEMO-EST-001']);
  }
  const [rows]=await c.query(`SELECT u.id,u.nombre,u.correo,u.estado,r.codigo rol,t.id tutor_id,e.id estudiante_id
    FROM usuarios u JOIN usuario_roles ur ON ur.usuario_id=u.id JOIN roles r ON r.id=ur.rol_id
    LEFT JOIN tutores t ON t.usuario_id=u.id LEFT JOIN estudiantes e ON e.usuario_id=u.id
    WHERE u.correo IN (?) ORDER BY u.id`,[accounts.map(a=>a.correo)]);
  if(rows.length!==3||!rows.find(r=>r.rol==='tutor'&&r.tutor_id)||!rows.find(r=>r.rol==='estudiante'&&r.estudiante_id))throw Error('Perfiles de prueba incompletos.');
  await c.commit();
  console.log(JSON.stringify({creados:rows,contrasenas:'bcrypt costo 12; hash y verificación correctos'},null,2));
 }catch(e){if(c)await c.rollback();console.error((e.code?e.code+': ':'')+e.message);process.exitCode=1;}
 finally{if(c)await c.end();}
})();
