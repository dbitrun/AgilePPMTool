package com.example.ppmtool.services;

import com.example.ppmtool.domain.Backlog;
import com.example.ppmtool.domain.Project;
import com.example.ppmtool.domain.ProjectTask;
import com.example.ppmtool.exceptions.ProjectNotFoundException;
import com.example.ppmtool.repositories.BacklogRepository;
import com.example.ppmtool.repositories.ProjectRepository;
import com.example.ppmtool.repositories.ProjectTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectTaskService {

    @Autowired
    private BacklogRepository backlogRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public ProjectTask addProjectTask(String projectIdentifier, ProjectTask projectTask) {

        try {
            // PTs to be added to a specific project, project != null, BL exists
            Backlog backlog = backlogRepository.findByProjectIdentifier(projectIdentifier);
            // set the Backlog to ProjectTask
            projectTask.setBacklog(backlog);
            // we want our project sequence to be like this: IDPRO-1 IDPRO-2... -100 101
            Integer backlogSequence = backlog.getPTSequence();
            // Update the Backlog SEQUENCE
            backlogSequence++;

            backlog.setPTSequence(backlogSequence);

            // Add Sequence to Project Task
            projectTask.setProjectSequence(projectIdentifier + "-" + backlogSequence);
            projectTask.setProjectIdentifier(projectIdentifier);

            // INITIAL status when status is null
            if (projectTask.getStatus() == null || projectTask.getStatus() == "") {
                projectTask.setStatus("TO_DO");
            }

            // INITIAL priority when priority null, we need to check null first
            if (projectTask.getPriority() == null || projectTask.getPriority() == 0) {
                projectTask.setPriority(3);
            }

            return projectTaskRepository.save(projectTask);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ProjectNotFoundException("Project not found!");
        }
    }

    public Iterable<ProjectTask> findBacklogById(String id) {

        Project project = projectRepository.findByProjectIdentifier(id);

        if (project == null) {
            throw new ProjectNotFoundException("Project with ID '" + id + "' does not exists");
        }

        return projectTaskRepository.findByProjectIdentifierOrderByPriority(id);
    }

    public ProjectTask findPTByProjectSequence(String backlog_id, String pt_id) {

        // make sure we are searching on the right backlog
        Backlog backlog = backlogRepository.findByProjectIdentifier(backlog_id);
        if (backlog == null) {
            throw new ProjectNotFoundException("Project with ID '" + backlog_id + "' does not exists");
        }

        // make sure that our task exists
        ProjectTask projectTask = projectTaskRepository.findByProjectSequence(pt_id);
        if (projectTask == null) {
            throw new ProjectNotFoundException("Project Task '" + pt_id + "' not found");
        }

        // make sure that the backlog/project id in the path corresponds to the right project
        if (!projectTask.getProjectIdentifier().equals(backlog_id)) {
            throw new ProjectNotFoundException("Project Task '" + pt_id + "' does not exist in project '" + backlog_id + "'");
        }

        return projectTask;
    }

    // Update project task
    public ProjectTask updateByProjectSequence(ProjectTask updatedTask, String backlog_id, String pt_id) {
        // find existing project task
        // ProjectTask projectTask = projectTaskRepository.findByProjectSequence(updatedTask.getProjectSequence());
        // there are all validations in findPTByProjectSequence already
        ProjectTask projectTask = findPTByProjectSequence(backlog_id, pt_id);

        // Additional validation, check if pt_id in url is equal to projectSequence in json body
        if (!pt_id.equals(updatedTask.getProjectSequence())) {
            // System.out.println("ProjectTask in URL:  " + pt_id + "\nProjectTask in Body: " + updatedTask.getProjectSequence());
            throw new ProjectNotFoundException("Project Task ID in URL '" + pt_id + "' is not equals to projectSequence in JSON Body '" + updatedTask.getProjectSequence() + "'");
        }

        // replace it with updated task
        projectTask = updatedTask;

        // save update
        return projectTaskRepository.save(projectTask);
    }

    public void deletePTByProjectSequence(String backlog_id, String pt_id) {
        ProjectTask projectTask = findPTByProjectSequence(backlog_id, pt_id);

        /*
        // This code is required, if following relationship is setting for property projectTask in Backlog class
        // @OneToMany(cascade = CascadeType.ALL, ...)
        // With following relationship
        // @OneToMany(cascade = CascadeType.REFRESH, ..., , orphanRemoval=true)
        // this code is not required
        Backlog backlog = projectTask.getBacklog();
        List<ProjectTask> pts = backlog.getProjectTasks();
        pts.remove(projectTask);
        backlogRepository.save(backlog);
        */

        projectTaskRepository.delete(projectTask);
    }

}
