# Gitlab Cloner

This is a tool that can be used to clone a whole Gitlab Group at once.
To do so, create an access token (https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html).
The token needs at least API and Read Repository access.
Then, you can run this tool with the following options:

 - "--host" or "-h": The host url. For example "https://gitlab.com" (this is also the default)
 - "--token" or "-t": The gitlab access token
 - "--group" or "-g": The group id (as seen under the group name in gitlab)
 - "--folder" or "-f": The target folder for the repositories to be cloned into. This defaults to the current working directory.

What you need in order to run this application:

 - A current jre (This was created with JDK version 17)
 - Git installed (As the application calls java process exec for git clone)

The process fails if the target folder already contains any of the folders it would create, if any folder creation fails, or if any cloning fails.
The process creates a folder structure that fits the Gitlab Group's subgroup structure, and recursively adds all projects in all subgroups.
