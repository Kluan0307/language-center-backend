import {useState} from 'react';
import {Alert,App,Button,Checkbox,Input,Select,Space,Table,Typography} from 'antd';
import {useRemoteResource} from '../../../hooks/useRemoteResource';
import {getErrorMessage} from '../../../utils/apiErrors';
import {LoadError} from '../Academic';
import {gradingTransitionService} from '../../../services/gradingTransitionService';
import type {LegacyMapping,LegacyPreview} from '../../../services/gradingTransitionService';
import type {GradingTemplate} from '../../../services/gradingService';
export default function GradingMigrationPanel({classId,templates,onSaved}:{classId:number;templates:GradingTemplate[];onSaved:()=>void}){
 const {message}=App.useApp();const source=useRemoteResource(gradingTransitionService.source,{classId});const [templateId,setTemplateId]=useState<string>();const [mapping,setMapping]=useState<Record<number,string>>({});const [reason,setReason]=useState('');const [preview,setPreview]=useState<{data:LegacyPreview;requestId:string}|null>(null);const [checked,setChecked]=useState(false);const [busy,setBusy]=useState(false);const chosen=templates.find(t=>t.id===templateId);
 const plan=()=>({templateId:templateId!,mappings:(source.data?.exams??[]).map(e=>({examId:e.id,component:mapping[e.id]})) as LegacyMapping[]});
 async function inspect(){if(!templateId||reason.trim().length<3||source.data?.exams.some(e=>!mapping[e.id])){message.error('Chọn quy chế, ánh xạ từng bài và nhập lý do trước khi xem trước.');return;}setBusy(true);try{const data=await gradingTransitionService.preview(classId,plan());setPreview({data,requestId:crypto.randomUUID()});setChecked(false);}catch(e){message.error(getErrorMessage(e));}finally{setBusy(false);}}
 async function apply(){if(!preview||!checked)return;setBusy(true);try{await gradingTransitionService.apply(classId,{requestId:preview.requestId,plan:plan(),fingerprint:preview.data.fingerprint,reason});message.success('Đã sao chép điểm để đối chiếu. Điểm và công bố cũ giữ nguyên; kết quả mới chưa công bố.');onSaved();}catch(e){message.error(getErrorMessage(e));source.reload();}finally{setBusy(false);}}
 if(source.loading)return <Typography.Paragraph>Đang kiểm tra dữ liệu điểm cũ…</Typography.Paragraph>;
 if(source.error)return <LoadError error={source.error} reload={source.reload}/>;
 if(!source.data?.exams.length||source.data.alreadyMigrated)return null;
 return <Space orientation="vertical" style={{width:'100%',marginTop:16}}><Alert type="warning" showIcon title="Lớp có điểm theo cấu trúc cũ" description="Giữ nguyên để tiếp tục dùng, hoặc chọn ánh xạ và xem trước trước khi sao chép sang quy chế mới. Không tự tách điểm tổng IELTS/TOEIC thành kỹ năng."/>
 <Select style={{width:'100%',maxWidth:460}} placeholder="Chọn quy chế điểm nội bộ để đối chiếu" value={templateId} onChange={v=>{setTemplateId(v);setMapping({});setPreview(null);}} options={templates.filter(t=>t.code==='INTERNAL').map(t=>({value:t.id,label:t.name+' · v'+t.revision}))}/>
 <Table rowKey="id" size="small" pagination={false} dataSource={source.data.exams} scroll={{x:650}} columns={[{title:'Bài cũ',dataIndex:'name'},{title:'Thang',dataIndex:'maxScore'},{title:'Điểm đã nhập',dataIndex:'grades'},{title:'Ánh xạ sang',render:(_,e)=><Select style={{minWidth:190}} placeholder="Chọn thành phần" value={mapping[e.id]} onChange={v=>{setMapping(m=>({...m,[e.id]:v}));setPreview(null);}} options={chosen?.spec.components.map(c=>({value:c.code,label:c.name+' · thang '+c.maxScore}))}/>}]} />
 <Input.TextArea rows={2} maxLength={500} placeholder="Lý do chuyển đổi và căn cứ chọn ánh xạ" value={reason} onChange={e=>{setReason(e.target.value);setPreview(null);}}/>
 <Button loading={busy} onClick={()=>void inspect()}>Xem trước đối chiếu</Button>
 {preview&&<><Alert type={preview.data.blockers.length?'error':'info'} showIcon title={`${preview.data.exams} bài · ${preview.data.grades} điểm gốc`} description={preview.data.blockers.length?preview.data.blockers.join('; '):'Cùng thang điểm. Kết quả dưới đây được tính theo quy chế mới; không ghi đè điểm hoặc công bố cũ.'}/>
 {preview.data.proposed&&<Table rowKey="enrollmentId" size="small" dataSource={preview.data.proposed.students} pagination={{pageSize:5}} columns={[{title:'Học viên',dataIndex:'studentName'},{title:'Điểm dự kiến (%)',render:(_,r)=>r.score===null?'Chưa đủ dữ liệu':r.score},{title:'Cần kiểm tra',render:(_,r)=>r.reasons.join('; ')||'Đủ dữ liệu tính toán'}]}/>}
 {!preview.data.blockers.length&&<><Checkbox checked={checked} onChange={e=>setChecked(e.target.checked)}>Tôi đã kiểm tra thang điểm, ánh xạ và kết quả dự kiến. Chỉ sao chép thành bản nháp.</Checkbox><Button type="primary" disabled={!checked} loading={busy} onClick={()=>void apply()}>Xác nhận sao chép điểm</Button></>}
 </>}</Space>;
}
