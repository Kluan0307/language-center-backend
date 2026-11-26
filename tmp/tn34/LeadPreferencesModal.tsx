import {useState} from 'react';
import {Alert,Button,Form,Input,Modal,Select,Space,Typography} from 'antd';
import {useRemoteResource} from '../../hooks/useRemoteResource';
import {useFormLeaveGuard} from '../../hooks/useFormLeaveGuard';
import {classService,courseService} from '../../services/academicService';
import {leadIntakeService} from '../../services/leadIntakeService';
import type {LeadDto} from '../../types/admissions';
import {LoadError} from '../../components/Common/Academic';
import {getErrorMessage} from '../../utils/apiErrors';
import {displayDate} from '../../utils/display';
async function load({branchId}:{branchId:number},signal?:AbortSignal){const [classes,courses]=await Promise.all([classService.all({branchId},signal),courseService.all({branchId},signal)]);return {classes,courses};}
export default function LeadPreferencesModal({lead,onClose,onSaved}:{lead:LeadDto;onClose:()=>void;onSaved:(lead:LeadDto)=>void}){
 const [form]=Form.useForm();const [busy,setBusy]=useState(false);const [error,setError]=useState<string>();const guard=useFormLeaveGuard(busy,onClose);const options=useRemoteResource(load,{branchId:lead.branchId});const preferences=useRemoteResource(leadIntakeService.preferences,{id:lead.id});const courseId=Form.useWatch('courseId',form) as number|undefined;
 async function save(){if(busy)return;try{const v=await form.validateFields();setBusy(true);setError(undefined);onSaved(await leadIntakeService.preference(lead.id,{version:lead.version,courseId:v.courseId??null,classId:v.classId??null,preferredTime:v.preferredTime?.trim()||null,reason:v.reason}));}catch(e){if(!(e&&typeof e==='object'&&'errorFields'in e))setError(getErrorMessage(e));}finally{setBusy(false);}}
 return <Modal open title="Nhu cầu khóa học và lớp học" width={650} onCancel={guard.cancel} onOk={()=>void save()} okText="Lưu nhu cầu" confirmLoading={busy} closable={!busy} maskClosable={!busy} cancelButtonProps={{disabled:busy}}><Space orientation="vertical" style={{width:'100%'}}><Alert type="info" showIcon title="Lớp mong muốn chưa phải đăng ký, chưa giữ chỗ"/><LoadError error={options.error} reload={options.reload}/><LoadError error={preferences.error} reload={preferences.reload}/>{error&&<Alert type="warning" title={error} showIcon/>}
 <Form form={form} layout="vertical" disabled={busy} onValuesChange={guard.changed} initialValues={{courseId:lead.interestedCourseId??undefined,classId:lead.preferredClassId??undefined,preferredTime:lead.preferredTime??'',reason:''}}>
 <Form.Item name="courseId" label="Khóa học quan tâm"><Select allowClear showSearch optionFilterProp="label" loading={options.loading} options={options.data?.courses.map(c=>({value:c.id,label:c.code+' · '+c.name}))} onChange={()=>form.setFieldValue('classId',undefined)}/></Form.Item>
 <Form.Item name="classId" label="Lớp mong muốn"><Select allowClear showSearch optionFilterProp="label" loading={options.loading} options={options.data?.classes.filter(c=>!courseId||c.courseId===courseId).map(c=>({value:c.id,label:c.code+' · '+c.name}))} onChange={id=>{const selected=options.data?.classes.find(c=>c.id===id);if(selected)form.setFieldValue('courseId',selected.courseId);}}/></Form.Item>
 <Form.Item name="preferredTime" label="Khung giờ phù hợp" rules={[{max:500}]}><Input.TextArea rows={2} maxLength={500} placeholder="Ví dụ: buổi tối thứ 2, 4, 6"/></Form.Item><Form.Item name="reason" label="Lý do cập nhật" rules={[{required:true,whitespace:true,min:3,max:500}]}><Input.TextArea rows={2} maxLength={500}/></Form.Item></Form>
 {!!preferences.data?.websiteChoices.length&&<><Typography.Text strong>Các yêu cầu từ website gần đây</Typography.Text>{preferences.data.websiteChoices.slice(0,5).map((w,i)=><Typography.Paragraph key={i} type="secondary">{displayDate(w.submittedAt)} · {w.classSlug??w.courseSlug??w.programSlug??'Tư vấn chung'}</Typography.Paragraph>)}</>}
 <Typography.Text type="secondary">Bỏ chọn rồi lưu để xóa nhu cầu cũ. Lịch sử trước và sau được giữ lại.</Typography.Text><Button onClick={()=>{options.reload();preferences.reload();}}>Tải lại lựa chọn</Button></Space></Modal>;
}
