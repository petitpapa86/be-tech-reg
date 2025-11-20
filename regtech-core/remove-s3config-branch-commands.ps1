# Run these commands in PowerShell from the repository root to create a branch, remove deprecated S3Configuration, commit and push.

git checkout -b chore/remove-report-s3config

git rm "regtech-report-generation/infrastructure/src/main/java/com/bcbs239/regtech/reportgeneration/infrastructure/config/S3Configuration.java"

git add -A

git commit -m "chore: remove deprecated module-local S3Configuration; use core S3Config"

git push -u origin chore/remove-report-s3config

# After pushing, open a PR on your Git hosting provider to merge the branch into main/master.
# Then run a build locally to verify everything:
#mvn -DskipTests package
# Optionally run tests:
#mvn test

