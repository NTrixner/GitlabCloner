package eu.trixner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        ArgParams params = getParams(args);

        try {
            // Create a GitLabApi instance to communicate with your GitLab server
            GitLabApi gitLabApi = new GitLabApi(params.getHost(), params.getToken());

            HashMap<String, List<Project>> nameSpacesAndProjects = new HashMap<>();
            // Get the list of projects your account has access to
            Group glGroup = gitLabApi.getGroupApi().getGroup(params.getGroupId());
            getProjectsForGroupRecursive(gitLabApi, glGroup, nameSpacesAndProjects, "");

            for (Map.Entry<String, List<Project>> entry : nameSpacesAndProjects.entrySet()) {
                Path path = Path.of(params.getTargetFolder(), entry.getKey());
                for (Project p : entry.getValue()) {
                    System.out.println(path + "\\" + p.getName());
                    File f = new File(path.toUri());
                    if (f.exists() || f.mkdir()) {
                        ProcessBuilder pb = new ProcessBuilder("git", "clone", p.getSshUrlToRepo());
                        pb.directory(f);
                        pb.inheritIO();
                        Process process = pb.start();
                        int code = process.waitFor();
                        if (code != 0) {
                            throw new InterruptedException("Git clone failed!");
                        }
                    } else {
                        throw new FileSystemException("Could not create file");
                    }
                }
            }
        } catch (GitLabApiException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (FileSystemException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }


    }

    private static void getProjectsForGroupRecursive(GitLabApi gitLabApi, Group glGroup, HashMap<String, List<Project>> projects, String namespacePath) throws GitLabApiException {
        List<Project> projectsGroup = gitLabApi.getGroupApi().getProjects(glGroup.getId());
        if (projectsGroup != null) {
            projects.put(namespacePath, new ArrayList<>(projectsGroup));
        }
        List<Group> subGroups = gitLabApi.getGroupApi().getSubGroups(glGroup.getId());
        if (subGroups != null) {
            for (Group g : subGroups) {
                getProjectsForGroupRecursive(gitLabApi, g, projects, namespacePath + "\\" + g.getPath());
            }
        }
    }

    private static ArgParams getParams(String[] args) {
        ArgParams params = new ArgParams();

        Options options = new Options();
        Option hostOption = new Option("h", "host", true, "Host URL");
        hostOption.setRequired(false);
        options.addOption(hostOption);

        Option tokenOption = new Option("t", "token", true, "Access Token");
        tokenOption.setRequired(true);
        options.addOption(tokenOption);

        Option groupOption = new Option("g", "group", true, "Group ID");
        groupOption.setRequired(true);
        options.addOption(groupOption);

        Option folderOption = new Option("f", "folder", true, "Target Folder");
        folderOption.setRequired(false);
        options.addOption(folderOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
            String host = cmd.getOptionValue("host");
            if (host == null) {
                host = "http://gitlab.com";
            }
            String folder = cmd.getOptionValue("folder");
            if (folder == null) {
                folder = "./";
            }

            String token = cmd.getOptionValue("token");
            String group = cmd.getOptionValue("group");

            params.setHost(host);
            params.setToken(token);
            params.setTargetFolder(folder);
            params.setGroupId(group);

            return params;
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        return null;
    }
}
