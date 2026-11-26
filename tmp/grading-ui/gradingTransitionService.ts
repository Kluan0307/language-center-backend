import {api} from '../api/axiosInstance';
import type {ResultReport,ResultRow,GradingActivity} from './gradingService';
import type {ClassFinals} from './finalResultService';
export interface LegacyMapping {examId:number;component:string}
export interface LegacyPlan {templateId:string;mappings:LegacyMapping[]}
export interface LegacySource {classId:number;exams:{id:number;name:string;examType:string;date:string;maxScore:number;grades:number;publishedVersions:number}[];alreadyMigrated:boolean;notices:string[]}
export interface LegacyPreview {classId:number;templateId:string;fingerprint:string;blockers:string[];exams:number;grades:number;proposed:ResultReport|null}
export interface CompletionPreview {classId:number;fingerprint:string;students:ResultRow[];blockers:string[];current:ClassFinals}
const base='/teaching/grading/classes/';
export const gradingTransitionService={
 async source({classId}:{classId:number},signal?:AbortSignal){return(await api.get<LegacySource>(base+classId+'/legacy/source',{signal})).data;},
 async preview(classId:number,plan:LegacyPlan){return(await api.post<LegacyPreview>(base+classId+'/legacy/preview',plan)).data;},
 async apply(classId:number,data:{requestId:string;plan:LegacyPlan;fingerprint:string;reason:string}){return(await api.post(base+classId+'/legacy/apply',data)).data;},
 async completion({classId}:{classId:number;revision?:number},signal?:AbortSignal){return(await api.post<CompletionPreview>(base+classId+'/completion/preview',undefined,{signal})).data;},
 async confirm(classId:number,data:{requestId:string;fingerprint:string;reason:string}){return(await api.post<ClassFinals>(base+classId+'/completion/confirm',data)).data;},
 async edit(classId:number,id:number,data:{expectedVersion:number;name:string;date:string;reason:string}){return(await api.patch<GradingActivity>(base+classId+'/activities/'+id,data)).data;},
 async export(classId:number,published:boolean){const blob=(await api.get<Blob>(base+classId+'/export',{params:{published},responseType:'blob'})).data;const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`bang-diem-${classId}-${published?'cong-bo':'nhap'}.csv`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);},
};
