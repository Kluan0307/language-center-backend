const fs=require('fs'),path=require('path');const base='academic-service/src/main/java/com/example/academic/';
function edit(p,f){p=base+p;fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));}
edit('lead/Lead.java',s=>s.replace('  private String desiredLevel;',`  private String desiredLevel;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="preferred_class_id")
  private com.example.academic.schoolclass.SchoolClass preferredClass;
  @Column(length=500) private String preferredTime;`));
edit('lead/LeadDto.java',s=>s.replace('boolean overdue) {','boolean overdue, Long preferredClassId, String preferredClassName, String preferredTime) {').replace('.isBefore(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE)));','.isBefore(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE)), l.getPreferredClass()==null?null:l.getPreferredClass().getId(), l.getPreferredClass()==null?null:l.getPreferredClass().getName(), l.getPreferredTime());').replace('@PositiveOrZero Long version) {','@PositiveOrZero Long version, @Positive Long preferredClassId, @Size(max=500) String preferredTime) {\n    public Request(String fullName,String phone,String email,String source,Long interestedCourseId,String desiredLevel,String note,Long branchId,Long version){this(fullName,phone,email,source,interestedCourseId,desiredLevel,note,branchId,version,null,null);}'));
edit('lead/LeadService.java',s=>s.replace('  private Lead lock(', '  Lead lock(').replace('  private void owner(', '  void owner(').replace('  private void record(', '  void record(').replace('    if (id == null) {\n      var u',`    if(r.preferredClassId()!=null){var target=entityManager.find(com.example.academic.schoolclass.SchoolClass.class,r.preferredClassId());if(target==null||!target.getBranchId().equals(branch))throw ApiException.bad("Lớp mong muốn không thuộc chi nhánh");if(l.getInterestedCourse()!=null&&!l.getInterestedCourse().getId().equals(target.getCourse().getId()))throw ApiException.bad("Lớp không thuộc khóa học quan tâm");l.setPreferredClass(target);l.setInterestedCourse(target.getCourse());}
    if(r.preferredTime()!=null)l.setPreferredTime(r.preferredTime().trim());
    if(l.getPreferredClass()!=null&&l.getInterestedCourse()!=null&&!l.getPreferredClass().getCourse().getId().equals(l.getInterestedCourse().getId()))throw ApiException.bad("Cập nhật nhu cầu để bỏ hoặc đổi lớp mong muốn trước khi đổi khóa học");
    if (id == null) {\n      var u`));
edit('website/WebsiteIntakeService.java',s=>s.replace('    if(r.classSlug()!=null){','    SchoolClass preferredClass=null;\n    if(r.classSlug()!=null){').replace('var sc=em.find(SchoolClass.class,cp.getSourceId());','var sc=em.find(SchoolClass.class,cp.getSourceId());preferredClass=sc;').replace('lead.setInterestedCourse(course);lead.setNote(description);','lead.setInterestedCourse(course);lead.setPreferredClass(preferredClass);lead.setNote(description);'));
edit('placement/PlacementService.java',s=>s.replace('    operations();\n    if (!Objects.equals(s.getId(), r.studentId())',`    return evaluate(s,c,r,false);
  }
  public PlacementDto.Preview previewProspect(Student prospect,Long classId){
    if(prospect.getId()==null||prospect.getId()>=0)throw ApiException.bad("Sai hồ sơ xem trước");
    return evaluate(prospect,classes.require(classId),context(prospect.getId(),classId,PlacementDto.Intent.ENROLL,null),true);
  }
  private PlacementDto.Preview evaluate(Student s, SchoolClass c, PlacementDto.Context r,boolean prospect) {
    operations();
    if (!Objects.equals(s.getId(), r.studentId())`).replace('    students.require(s.getId());','    if(!prospect)students.require(s.getId());'));
console.log('Lead preferences and read-only prospective placement prepared.');
