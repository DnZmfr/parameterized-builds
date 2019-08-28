package com.kylenicholls.stash.parameterizedbuilds.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.rest.RestResource;
import com.atlassian.bitbucket.rest.util.RestUtils;
import com.atlassian.bitbucket.setting.Settings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kylenicholls.stash.parameterizedbuilds.helper.SettingsService;
import com.kylenicholls.stash.parameterizedbuilds.item.Job;
import com.sun.jersey.spi.resource.Singleton;

@Path("/projects/{projectKey}/repos/{repoSlug}")
@Singleton
public class RepositoryResource extends RestResource {

    private final AuthenticationContext authContext;
    private final SettingsService settingsService;
    private final RepositoryService repositoryService;

    public RepositoryResource(I18nService i18nService, AuthenticationContext authContext,
                              SettingsService settingsService, 
                              RepositoryService repositoryService) {
        super(i18nService);
        this.authContext = authContext;
        this.settingsService = settingsService;
        this.repositoryService = repositoryService;
    }

    @GET
    @Path("/jobs")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ RestUtils.APPLICATION_JSON_UTF8 })
    public Response getJobs(@Context UriInfo ui){
        if (authContext.isAuthenticated()) {
            String projectKey = ui.getPathParameters().getFirst("projectKey");
            String repoSlug = ui.getPathParameters().getFirst("repoSlug");
            Repository repository = repositoryService.getBySlug(projectKey, repoSlug);
            Settings settings = settingsService.getSettings(repository);
            if (settings == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<Map<String, Object>> jobJson = settingsService.getJobs(settings.asMap()).stream()
                .map(job -> job.rawMap())
                .collect(Collectors.toList());

            return Response.ok(jobJson).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @GET
    @Path("/jobs/{jobId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ RestUtils.APPLICATION_JSON_UTF8 })
    public Response getJob(@Context UriInfo ui){
        if (authContext.isAuthenticated()) {
            String projectKey = ui.getPathParameters().getFirst("projectKey");
            String repoSlug = ui.getPathParameters().getFirst("repoSlug");
            String jobIdStr = ui.getPathParameters().getFirst("jobId");
            int jobId;
            try {
                jobId = Integer.parseInt(jobIdStr);
            } catch (NumberFormatException e) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Repository repository = repositoryService.getBySlug(projectKey, repoSlug);
            Settings settings = settingsService.getSettings(repository);
            if (settings == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<Job> jobs = settingsService.getJobs(settings.asMap());
            if (jobs.size() <= jobId) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Map<String, Object> jobJson = jobs.get(jobId).rawMap();

            return Response.ok(jobJson).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}