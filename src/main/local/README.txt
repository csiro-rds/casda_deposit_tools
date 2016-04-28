This directory contains local config files only.
The directory will be configured to be in Eclipse's
classpath and any deployToLocal gradle script will
incorporate them into the local deployed app.
These files will not (must not) be deployed to
server builds.

(Note: this would be better placed as a sub-directory
of resources but Eclipse has a limitation on how we
can configure classpath excludes and includes that
makes that impossible.)