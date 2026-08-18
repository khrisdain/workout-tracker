package com.fitnesstracker.workouttracker.service;

import com.fitnesstracker.workouttracker.exception.ResourceNotFoundException;
import com.fitnesstracker.workouttracker.model.Program;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.repository.ProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coach authored training blocks. Creation and editing are role gated in the
 * controller; ownership is enforced here so a coach cannot edit another coach's
 * program unless they are also an administrator.
 */
@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programs;

    public ProgramService(ProgramRepository programs) {
        this.programs = programs;
    }

    public List<Program> findPublished() {
        return programs.findByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Program> findAll() {
        return programs.findAll();
    }

    public List<Program> findByCoach(String username) {
        return programs.findByCoachUsernameOrderByCreatedAtDesc(username);
    }

    public List<Program> findEnrolled(String username) {
        return programs.findEnrolled(username);
    }

    public long countEnrolled(String username) {
        return programs.countEnrolled(username);
    }

    public Program get(Long id) {
        return programs.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Program", id));
    }

    @Transactional
    public Program create(Program program, User coach) {
        program.setId(null);
        program.setCoach(coach);
        return programs.save(program);
    }

    @Transactional
    public Program update(Long id, Program edited) {
        Program existing = get(id);
        existing.setName(edited.getName());
        existing.setDescription(edited.getDescription());
        existing.setFocus(edited.getFocus());
        existing.setDifficulty(edited.getDifficulty());
        existing.setDurationWeeks(edited.getDurationWeeks());
        existing.setSessionsPerWeek(edited.getSessionsPerWeek());
        existing.setPublished(edited.isPublished());
        return programs.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        programs.delete(get(id));
    }

    @Transactional
    public boolean enrol(Long programId, User member) {
        Program program = get(programId);
        boolean added = program.getMembers().add(member);
        programs.save(program);
        return added;
    }

    @Transactional
    public boolean withdraw(Long programId, User member) {
        Program program = get(programId);
        boolean removed = program.getMembers().removeIf(m -> m.getId().equals(member.getId()));
        programs.save(program);
        return removed;
    }

    /** Administrators may edit anything, a coach only their own programs. */
    public boolean canModify(Program program, User actor) {
        if (actor == null) {
            return false;
        }
        if (actor.hasRole(Role.ADMIN)) {
            return true;
        }
        return actor.hasRole(Role.COACH)
                && program.getCoach() != null
                && program.getCoach().getId().equals(actor.getId());
    }

    public long count() {
        return programs.count();
    }
}
