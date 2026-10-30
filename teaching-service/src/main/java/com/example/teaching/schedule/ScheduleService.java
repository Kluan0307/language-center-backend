package com.example.teaching.schedule;

import com.example.teaching.entity.Room;
import com.example.teaching.entity.Schedule;
import com.example.teaching.entity.SchoolClass;
import com.example.teaching.room.RoomRepository;
import com.example.teaching.schoolclass.SchoolClassRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final RoomRepository roomRepository;

    public List<ScheduleResponse> getAll(Long schoolClassId, Long roomId, Long teacherAccountId, DayOfWeek dayOfWeek) {
        return scheduleRepository.findAll().stream()
                .filter(schedule -> schoolClassId == null || Objects.equals(schedule.getSchoolClass() != null ? schedule.getSchoolClass().getId() : null, schoolClassId))
                .filter(schedule -> roomId == null || Objects.equals(schedule.getRoom() != null ? schedule.getRoom().getId() : null, roomId))
                .filter(schedule -> teacherAccountId == null || Objects.equals(schedule.getTeacherAccountId(), teacherAccountId))
                .filter(schedule -> dayOfWeek == null || schedule.getDayOfWeek() == dayOfWeek)
                .map(ScheduleResponse::fromEntity)
                .toList();
    }

    public List<ScheduleResponse> getMySchedules(Long teacherAccountId) {
        if (teacherAccountId == null) {
            throw new IllegalArgumentException("teacherAccountId không được để trống");
        }

        return scheduleRepository.findByTeacherAccountId(teacherAccountId).stream()
                .map(ScheduleResponse::fromEntity)
                .toList();
    }

    public ScheduleResponse getById(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch học với id=" + id));
        return ScheduleResponse.fromEntity(schedule);
    }

    @Transactional
    public ScheduleResponse create(ScheduleRequest request) {
        validateTimes(request.startTime(), request.endTime());

        Schedule schedule = new Schedule();
        applyScheduleData(schedule, request, null);
        return ScheduleResponse.fromEntity(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch học với id=" + id));

        validateTimes(request.startTime(), request.endTime());
        applyScheduleData(schedule, request, id);
        return ScheduleResponse.fromEntity(scheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch học với id=" + id));
        scheduleRepository.delete(schedule);
    }

    private void applyScheduleData(Schedule schedule, ScheduleRequest request, Long excludeId) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.schoolClassId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với id=" + request.schoolClassId()));

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng học với id=" + request.roomId()));

        if (schoolClass.getBranch() != null && room.getBranch() != null
                && !Objects.equals(schoolClass.getBranch().getId(), room.getBranch().getId())) {
            throw new IllegalArgumentException("Phòng học không thuộc cùng chi nhánh với lớp học");
        }

        ensureNoConflict(schoolClass, room, request.dayOfWeek(), request.startTime(), request.endTime(), excludeId);

        schedule.setSchoolClass(schoolClass);
        schedule.setRoom(room);
        schedule.setTeacherAccountId(request.teacherAccountId());
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
    }

    private void ensureNoConflict(SchoolClass schoolClass, Room room, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Long excludeId) {
        List<Schedule> roomSchedules = scheduleRepository.findByRoom_IdAndDayOfWeek(room.getId(), dayOfWeek);
        for (Schedule existing : roomSchedules) {
            if (Objects.equals(existing.getId(), excludeId)) {
                continue;
            }
            if (timesOverlap(existing.getStartTime(), existing.getEndTime(), startTime, endTime)) {
                throw new IllegalArgumentException("Phòng học đã được đặt trong khung giờ này");
            }
        }

        List<Schedule> classSchedules = scheduleRepository.findBySchoolClass_IdAndDayOfWeek(schoolClass.getId(), dayOfWeek);
        for (Schedule existing : classSchedules) {
            if (Objects.equals(existing.getId(), excludeId)) {
                continue;
            }
            if (timesOverlap(existing.getStartTime(), existing.getEndTime(), startTime, endTime)) {
                throw new IllegalArgumentException("Lớp học đã có lịch trùng thời gian này");
            }
        }

    }

    private void validateTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime và endTime không được để trống");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime phải lớn hơn startTime");
        }
    }

    private boolean timesOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
