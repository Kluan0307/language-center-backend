import {api} from '../api/axiosInstance';
export type GradingKind='OBSERVATION'|'INTERNAL'|'IELTS'|'TOEIC_LR';
export type MarkStatus='GRADED'|'ABSENT'|'EXEMPT'|'NOT_SUBMITTED';
export type ObservationLevel='NEEDS_SUPPORT'|'DEVELOPING'|'MEETS_EXPECTATIONS';
export interface GradingComponent {code:string;name:string;required:boolean;minScore:number|null;maxScore:number|null;step:number|null;weight:number|null}
export interface GradingSpec {kind:GradingKind;components:GradingComponent[];passPercent:number|null;minAttendancePercent:number|null;targetScore?:number|null;minimumSkillScore?:number|null}
export interface GradingTemplate {id:string;code:GradingKind;revision:number;name:string;spec:GradingSpec}
export interface GradingBinding {classId:number;branchId:number;courseId:number;revision:number;templateId:string;name:string;spec:GradingSpec;locked:boolean;assignedAt:string}
export interface GradingActivity {id:number;classId:number;name:string;date:string;components:string[];rosterFrozen:boolean;rosterSize:number;version:number;retakeOf:number|null;conversionId:number|null}
export interface GradingMark {studentId:number;component:string;version:number;status:MarkStatus;score:number|null;level:ObservationLevel|null;comment:string|null;enteredBy:number;updatedAt:string;rawScore:number|null;conversionId:number|null}
export interface GradingCandidate {studentId:number;studentName:string;enrollmentId:number}
export interface GradingSheet {binding:GradingBinding;activity:GradingActivity;students:GradingCandidate[];records:GradingMark[]}
export interface CourseGradingDefault {courseId:number;branchId:number;templateId:string;revision:number}
export interface GradingEvent {id:number;kind:string;actorId:number;occurredAt:string;reason:string}
const base='/teaching/grading';
export const gradingService={
 async workspace({classId}:{classId:number},signal?:AbortSignal){const [templates,binding,activities]=await Promise.all([api.get<GradingTemplate[]>(base+'/templates',{signal}),api.get<GradingBinding|null>(base+'/classes/'+classId+'/binding',{signal}),api.get<GradingActivity[]>(base+'/classes/'+classId+'/activities',{signal})]);return {templates:templates.data,binding:binding.data||null,activities:activities.data};},
 async assign(classId:number,templateId:string,expectedRevision:number,reason:string){return (await api.put<GradingBinding>(base+'/classes/'+classId+'/binding',{templateId,expectedRevision,reason})).data;},
 async revise(data:{code:GradingKind;expectedRevision:number;name:string;spec:GradingSpec;reason:string}){return(await api.post<GradingTemplate>(base+'/templates',data)).data;},
 async courseDefault(courseId:number,branchId:number){return(await api.get<CourseGradingDefault|null>(base+'/course-default',{params:{courseId,branchId}})).data||null;},
 async setDefault(data:{courseId:number;branchId:number;templateId:string;expectedRevision:number;reason:string}){return(await api.put<CourseGradingDefault>(base+'/course-default',data)).data;},
 async create(classId:number,data:{name:string;date:string;components:string[];retakeOf?:number;conversionId?:number;studentIds?:number[]}){return(await api.post<GradingActivity>(base+'/classes/'+classId+'/activities',data)).data;},
 async sheet({classId,id}:{classId:number;id:number|null},signal?:AbortSignal):Promise<GradingSheet|null>{return id===null?null:(await api.get<GradingSheet>(base+'/classes/'+classId+'/activities/'+id+'/sheet',{signal})).data;},
 async save(classId:number,id:number,data:{records:{studentId:number;component:string;expectedVersion:number|null;status:MarkStatus;score:number|null;level:ObservationLevel|null;comment:string|null;rawScore?:number|null}[];reason:string}){return(await api.put<GradingSheet>(base+'/classes/'+classId+'/activities/'+id+'/marks',data)).data;},
 async conversions(_params:object={},signal?:AbortSignal){return(await api.get<GradingConversion[]>(base+'/conversions',{signal})).data;},
 async createConversion(data:{code:string;expectedRevision:number;name:string;reference:string;mapping:{listening:number[];reading:number[]}}){return(await api.post<GradingConversion>(base+'/conversions',data)).data;},
 async history({classId}:{classId:number},signal?:AbortSignal){return(await api.get<GradingEvent[]>(base+'/classes/'+classId+'/history',{signal})).data;},
};
export interface GradingConversion {id:number;code:string;revision:number;name:string;reference:string;mapping:{listening:number[];reading:number[]}}
export interface ResultAttempt {activityId:number;name:string;date:string;status:string;score:number|null;rawScore:number|null;conversionId:number|null;level:string|null;comment:string|null;selected:boolean}
export interface ResultItem {rootId:number;activityId:number;name:string;date:string;status:string;score:number|null;rawScore:number|null;conversionId:number|null;level:string|null;comment:string|null;attempts:number[];history?:ResultAttempt[]|null}
export interface ResultPart {code:string;name:string;weight:number|null;score:number|null;maxScore:number|null;percent:number|null;level:string|null;status:string;assessments:ResultItem[]}
export interface ResultRow {enrollmentId:number;studentId:number;studentName:string;kind:GradingKind;score:number|null;unit:string;academicStatus:string;completionStatus:string;attendance:{status:string;percent:number|null;attended:number;expected:number;excusedAbsences:number;missingRecords:number};components:ResultPart[];reasons:string[];mockActivityId:number|null}
export interface ResultReport {classId:number;templateId:string;schemeName:string;spec:GradingSpec;calculationVersion:string;asOf:string;students:ResultRow[];rules:string[]}
export interface ResultWorkflow {state:string;revision:number;submittedBy:number|null;actorId:number|null;occurredAt:string|null;reason:string|null;sourceChanged:boolean}
export interface ResultPreview {report:ResultReport;fingerprint:string;workflow:ResultWorkflow}
export type WorkflowAction='SUBMIT'|'APPROVE'|'REJECT'|'PUBLISH'|'RETRACT'|'REOPEN';
export const gradingOutcomeService={
 async preview({classId}:{classId:number;revision?:number},signal?:AbortSignal){return(await api.get<ResultPreview>(base+'/classes/'+classId+'/preview',{signal})).data;},
 async history({classId}:{classId:number},signal?:AbortSignal){return(await api.get<ResultWorkflow[]>(base+'/classes/'+classId+'/workflow/history',{signal})).data;},
 async action(classId:number,action:WorkflowAction,command:{requestId:string;expectedRevision:number;fingerprint:string;reason:string}){return(await api.post<ResultPreview>(base+'/classes/'+classId+'/workflow/'+action,command)).data;},
};
