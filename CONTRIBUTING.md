Please familiarize yourself with the improvement process below in order to contribute to this project.

# Contributor Roles
One person may play multiple contributor roles in general. 
It is important to play exactly one role at any given moment. 

## Owner
Makes strategic decisions about the product, e.g. whether a certain behavior should be expected or not, weighing the benefit of a change versus the risk to destabilize the product.  

## Contributor
Makes changes to the product by submitting *pull requests*.

## Consumer
Consumes the product.
This role is most likely to discover bugs and opportunities for enhancement.


# Improvement Process
A proposed change goes through the following stages.

## Stage 1: New
A *consumer* runs into some unexpected behavior.

A new issue should be filed.

The issue must be labeled as: **stage-1-new**.

Each issue must have a *type* which is one of the following:

* *Bug* (defect) - Existing functionality that is broken.
* *Enhancement* - Missing functionality.
* *Question* - An alternative to a forum.

The issue must contain at least these 3 sections:

* REPRO  
The section must contain compilable, simple, code that consistently reproduces the unexpected behavior.
Additionally, it must contain instructions how to compile and run the repro code.

* EXPECTED  
A description of the expected behavior.

* ACTUAL  
A description of the unexpected behavior.
An explanation of why the actual behavior is considered unexpected my also be provided.

Once all the necessary information is in place, the owner(s) of the product will triage the issue.
It may be resolved with no further action, or it may be labeled as: **stage-2-triaged**.

### Ways to Contribute at This Stage
* File new issues when you run into behavior you didn't expect.
* Review issues in this stage, and flag duplicates by adding your notes to the issue description.
* Try to repro the issue. If the repro could be improved/simplified, add your improved repro. (Keep the original there too.)
You may optionally contact the author of the issue if more information is needed.

## Stage 2: Triaged
An issue in this stage is up for investigation. 

Investigation includes:

* Assessing the impact of the current behavior.
* Finding possible workarounds.
* Suggesting possible fixes.
* Assessing the cost and eventual side effects of each possible fix.

### Ways to Contribute at This Stage
* Pick an issue to investigate by labeling it as: **stage-3-investigating**.

## Stage 3: Investigating
The purpose of the investigation is to find the best resolution of this issue.

The outcome of the investigation should be recorded in two new sections:

* PROBLEM  
This section should contain a description of the cause of the observed behavior.

* SOLUTION  
This section should contain one or more possible solutions with some elaboration on possible side effects as well as a rough estimate on the cost.

Notice that "investigating" doesn't mean "fixing". 
While it may be necessary to hack some code to prove your concept for a fix, this is not the stage for a production-quality change.

Once a plausible fix has been found, the issue must be labeled as **stage-4-investigated**.

### Ways to Contribute at This Stage
* Repro the unexpected behavior. Rationalize the expectation. If the expectation is incorrect, *add* the necessary adjustments to the issue.
* Identify the code that causes the unexpected behavior.
* Describe possible fixes with their cost and eventual side effects. Asses whether a fix would be *breaking* to existing consumers.

## Stage 4: Investigated
A contributor has completed the investigation.
It is now up to the owner(s) to pick a proposed fix by calling out the chosen fix and labeling the issue as: **stage-5-approved**.

The owner(s) may return the issue for further investigation by labeling it as: **stage-2-triaged**. 

## Stage 5: Approved
A proposed fix for this issue has been approved that is now up for implementation.

### Ways to Contribute at This Stage
* Most typically, the contributor who did the investigation will implement the fix, unless that contributor specializes in investigations.
To mark the issue as being fixed, label it as: **stage-6-fixing**.

## Stage 6: Fixing
A contributor is implementing the approved fix for this issue. 
Once all of the following exit criteria have been met, the contributor labels the issue as: **stage-7-fixed**:

* The repro results in the expected behavior.
* A test for this behavior has been added.
* All tests are passing.
* A *pull request* is created. The pull request description must unambiguously identify the issue it is fixing.

## Stage 7: Fixed
An owner reviews the pull request, and if the issue has been correctly fixed, he/she pulls the fix and labels the issue as: **stage-8-closed**.

If the issue hasn't been fixed correctly, the owner may label is back as: **stage-6-fixing**, or even as: **stage-5-approved** if the implemented fix is completely wrong.

Now that a complete fix has been implemented, new side effects may appear. Therefore, the owner may close the issue with no further action.

## Stage 8: Closed
The issue has been fixed. 
The product has been improved.

