const fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process');
const action=process.argv[2];if(!['info','validate','baseline','migrate'].includes(action))throw Error('Uso: node database/flyway.cjs info|validate|baseline|migrate');
const c=JSON.parse(fs.readFileSync(path.join(__dirname,'../frontend/database/connection.local.json'),'utf8'));
const env={...process.env,FLYWAY_URL:process.env.FLYWAY_URL||('jdbc:mysql://'+c.host+':'+c.port+'/tutoria?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'),FLYWAY_USER:c.user,FLYWAY_PASSWORD:c.password};
const child=spawn('cmd.exe',['/d','/c','backend\\mvnw.cmd','-B','-ntp',...(process.env.FLYWAY_OFFLINE==='true'?['-o']:[]),'-f','database/pom.xml','org.flywaydb:flyway-maven-plugin:13.12.1:'+action],{cwd:path.join(__dirname,'..'),env,stdio:'inherit',windowsHide:true});child.on('error',()=>{console.error('No se pudo iniciar Maven');process.exitCode=1});child.on('exit',code=>{process.exitCode=code??1});
