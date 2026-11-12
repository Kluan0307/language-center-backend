const fs=require('fs'),path=require('path'),zlib=require('zlib');
const root=path.resolve(__dirname,'../../..'),app=path.resolve(root,'../../language-center-student-app'),dir=path.join(root,'storage/student-phase10');
const secrets=[];const text=fs.readFileSync(path.join(root,'.env.local'),'utf8');
for(const key of ['DB_PASSWORD','JWT_SECRET','VNPAY_HASH_SECRET','EXPO_PUSH_ACCESS_TOKEN']){const value=text.match(new RegExp('^'+key+'=(.*)$','m'))?.[1].trim();if(value&&value.length>=12)secrets.push({key,value});}
const banned=['Khám phá giao diện','Xem trước phát triển','9704198526191432198','EXPO_PUBLIC_PAYMENT_PROTOCOL_TEST'];
const report={checks:[],findings:[]};let expectedApi,apiFound=false;
function inspect(name,buffer){if(expectedApi&&(buffer.includes(Buffer.from(expectedApi))||buffer.includes(Buffer.from(expectedApi,'utf16le'))))apiFound=true;for(const {key,value} of secrets)if(buffer.includes(Buffer.from(value))||buffer.includes(Buffer.from(value,'utf16le')))report.findings.push({file:name,type:'server-secret',key});for(const value of banned)if(buffer.includes(Buffer.from(value))||buffer.includes(Buffer.from(value,'utf16le')))report.findings.push({file:name,type:'development-ui'});}
function walk(directory){for(const e of fs.readdirSync(directory,{withFileTypes:true})){const file=path.join(directory,e.name);if(e.isDirectory())walk(file);else inspect(path.relative(root,file),fs.readFileSync(file));}}
const exported=path.join(dir,'android-export');if(fs.existsSync(exported)){walk(exported);report.checks.push('Exported Android bundle/assets scanned');}
const latest=path.join(app,'artifacts/latest-android.json');
if(fs.existsSync(latest)){
 const meta=JSON.parse(fs.readFileSync(latest,'utf8')),file=path.join(app,'artifacts',meta.file),buf=fs.readFileSync(file);let cursor=0;expectedApi=meta.apiBaseUrl;
 // APK is a ZIP; inspect stored/deflated entries, not signing blocks or credentials.
 let eocd=-1;for(let n=buf.length-22;n>=Math.max(0,buf.length-65557);n--)if(buf.readUInt32LE(n)===0x06054b50){eocd=n;break;}
 if(eocd<0)throw Error('Invalid APK ZIP');cursor=buf.readUInt32LE(eocd+16);const count=buf.readUInt16LE(eocd+10);
 for(let n=0;n<count;n++){
   if(buf.readUInt32LE(cursor)!==0x02014b50)throw Error('Invalid ZIP entry');
   const method=buf.readUInt16LE(cursor+10),size=buf.readUInt32LE(cursor+20),len=buf.readUInt16LE(cursor+28),extra=buf.readUInt16LE(cursor+30),comment=buf.readUInt16LE(cursor+32),offset=buf.readUInt32LE(cursor+42),name=buf.subarray(cursor+46,cursor+46+len).toString();
   if(!name.endsWith('/')){const start=offset+30+buf.readUInt16LE(offset+26)+buf.readUInt16LE(offset+28),data=buf.subarray(start,start+size);if(method===0)inspect(name,data);else if(method===8)inspect(name,zlib.inflateRawSync(data));}
   cursor+=46+len+extra+comment;
 }
 report.checks.push('APK entries scanned for known backend secrets and development UI');report.apk=meta.file;
 report.apiBaseUrlEmbedded=apiFound;if(!apiFound)report.findings.push({type:'APK missing expected HTTPS API URL'});
}
report.status=report.findings.length?'FAIL':'PASS';fs.writeFileSync(path.join(dir,'artifact-scan.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));if(report.findings.length)process.exitCode=1;
