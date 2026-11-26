import {api} from '../api/axiosInstance';
import type {LeadDto,StudentRequest} from '../types/admissions';
import type {PlacementPreview} from './enrollmentPlacementService';
export type IntakeMode='PROFILE_ONLY'|'ENROLL';
export interface IntakeSelection {leadVersion:number;mode:IntakeMode;existingStudentId?:number;studentVersion?:number;profile?:StudentRequest;classId?:number;approvalId?:number;acknowledgeSimilarProfiles:boolean}
export interface IntakeCandidate {id:number;studentCode:string;fullName:string;dateOfBirth:string|null;phone:string;version:number}
export interface IntakePreview {lead:LeadDto;selection:IntakeSelection;profile:StudentRequest|null;similarProfiles:IntakeCandidate[];target:{id:number;code:string;name:string;branchId:number;courseId:number;startDate:string;endDate:string;tuitionFee:number;currentSize:number;maxSize:number}|null;placement:PlacementPreview|null;blockers:string[];notices:string[];fingerprint:string}
export interface IntakeCommand {requestId:string;selection:IntakeSelection;fingerprint:string;reason:string}
export interface IntakeResult {leadId:number;studentId:number;enrollmentId:number|null;mode:IntakeMode;createdStudent:boolean}
export interface LeadPreferences {current:LeadDto;websiteChoices:{programSlug:string|null;courseSlug:string|null;classSlug:string|null;submittedAt:string}[]}
export const leadIntakeService={
 async preferences({id}:{id:number},signal?:AbortSignal){return(await api.get<LeadPreferences>('/academic/leads/'+id+'/preferences',{signal})).data;},
 async preference(id:number,data:{version:number;courseId:number|null;classId:number|null;preferredTime:string|null;reason:string}){return(await api.put<LeadDto>('/academic/leads/'+id+'/preferences',data)).data;},
 async preview(id:number,selection:IntakeSelection){return(await api.post<IntakePreview>('/academic/leads/'+id+'/intake/preview',selection)).data;},
 async confirm(id:number,command:IntakeCommand){return(await api.post<IntakeResult>('/academic/leads/'+id+'/intake/confirm',command)).data;},
 async recover(id:number,requestId:string){return(await api.get<IntakeResult>('/academic/leads/'+id+'/intake/requests/'+requestId)).data;},
};
