import {useState} from 'react';
import {Alert,App,Button,Card,Input,Modal,Table,Tag} from 'antd';
import {api} from '../../../api/axiosInstance';
import {useRemoteResource} from '../../../hooks/useRemoteResource';
import {getErrorMessage} from '../../../utils/apiErrors';
import {gradingTransitionService} from '../../../services/gradingTransitionService';
import {finalResultService} from '../../../services/finalResultService';
import type {FinalSummary} from '../../../services/finalResultService';
import type {EnrollmentDto} from '../../../types/admissions';
import {LoadError} from '../Academic';
export default function GradingCompletionPanel({classId,revision}:{classId:number;revision:number}){
 const data=useRemoteResource(gradingTransitionService.completion,{classId,revision});const {message}=App.useApp();const [pending,setPending]=useState<{kind:'confirm';fingerprint:string;requestId:string}|{kind:'sync';record:EnrollmentDto;result:FinalSummary}|null>(null);const [reason,setReason]=useState('');const [busy,setBusy]=useState(false);const [synced,setSynced]=useState<number[]>([]);
 const confirmed=data.data?.current.results.filter(r=>r.state==='CONFIRMED')??[];
 async function sync(result:FinalSummary){setBusy(true);try{const record=(await api.get<EnrollmentDto>('/academic/enrollments/'+result.enrollmentId)).data;if(record.studyStatus==='COMPLETED'&&record.completionResultId===result.id&&record.completionResultRevision===result.revision){setSynced(s=>[...s,result.enrollmentId]);message.info('Đăng ký đã liên kết đúng kết quả hiện hành.');return;}setReason('');setPending({kind:'sync',record,result});}catch(e){message.error(getErrorMessage(e));}finally{setBusy(false);}}
 async function apply(){if(!pending||reason.trim().length<3){message.error('Nhập lý do ít nhất 3 ký tự.');return;}setBusy(true);try{if(pending.kind==='confirm')await gradingTransitionService.confirm(classId,{requestId:pending.requestId,fingerprint:pending.fingerprint,reason});else{await finalResultService.complete(pending.record,pending.result,reason);setSynced(s=>[...s,pending.record.id]);}setPending(null);data.reload();message.success('Đã cập nhật bước hoàn thành học vụ.');}catch(e){message.error(getErrorMessage(e));data.reload();}finally{setBusy(false);}}
 return <Card size="small" title="Kết thúc khóa học" style={{marginTop:16}} extra={<Button onClick={data.reload}>Kiểm tra điều kiện</Button>}><LoadError error={data.error} reload={data.reload}/>
 {data.data&&<><Alert type={confirmed.length?'success':data.data.blockers.length?'warning':'info'} showIcon title={confirmed.length?'Đã chốt kết quả chính thức':'Chốt kết quả đã công bố trước khi hoàn thành học vụ'} description={confirmed.length?'Hoàn thành đăng ký là bước riêng, không đổi học phí và không cấp chứng chỉ ngoại ngữ.':data.data.blockers.join('; ')||'Đủ điều kiện chốt kết quả cho các đăng ký đang học/hoàn thành.'}/>
 {!confirmed.length&&<Button type="primary" style={{marginBlock:12}} disabled={!!data.data.blockers.length} loading={busy} onClick={()=>{setReason('');setPending({kind:'confirm',fingerprint:data.data!.fingerprint,requestId:crypto.randomUUID()});}}>Chốt kết quả chính thức</Button>}
 {!!confirmed.length&&<Table rowKey="enrollmentId" size="small" style={{marginTop:12}} dataSource={confirmed} columns={[{title:'Học viên',render:(_,r)=>data.data?.students.find(s=>s.enrollmentId===r.enrollmentId)?.studentName??'Đăng ký #'+r.enrollmentId},{title:'Kết quả',render:(_,r)=>r.outcome==='ASSESSED'?'Đã hoàn thành đánh giá':r.outcome==='PASS'?'Đạt':'Chưa đạt'},{title:'Học vụ',render:(_,r)=>synced.includes(r.enrollmentId)?<Tag color="green">Đã liên kết hoàn thành</Tag>:<Button size="small" loading={busy} onClick={()=>void sync(r)}>Kiểm tra / hoàn thành</Button>}]} pagination={{pageSize:10}}/>}
 </>}
 <Modal open={!!pending} title={pending?.kind==='sync'?'Hoàn thành đăng ký học':'Chốt kết quả chính thức toàn lớp'} onCancel={()=>setPending(null)} onOk={()=>void apply()} confirmLoading={busy}><Alert type="info" showIcon title={pending?.kind==='sync'?'Xác nhận đã kết thúc quá trình học và liên kết kết quả hiện hành.':'Chốt bản đã duyệt/công bố. Muốn sửa nguồn sau đó phải mở lại kết quả toàn lớp.'} style={{marginBottom:12}}/><Input.TextArea rows={3} maxLength={500} value={reason} onChange={e=>setReason(e.target.value)} placeholder="Lý do xác nhận"/></Modal>
 </Card>;
}
