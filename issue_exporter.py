#!/usr/bin/python
#
# Copyright 2011 James Purser
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


""" This script imports issues from Code.Google and builds a csv file.
The intent is to imported the csv file into JIRA.

In order to run the script you will need the following library:

GData: http://code.google.com/p/gdata-python-client/"""

import gdata.projecthosting.client

client = gdata.projecthosting.client.ProjectHostingClient()
# Grab the first 25 issues
# TODO: figure out why we only get 25 issues and how to get the rest
feed = client.get_issues("wave-protocol")

f = open("issues.csv", "w")

# First row with column titles
f.write("Title,Id,Type,Priority,Status,Description\n")

# Append one row per issue
for issue in feed.entry:
    # Title
    row = "\"" + issue.title.text + "\","
    # Id, e.g. http://code.google.com/feeds/issues/p/wave-protocol/issues/full/1
    row += issue.id.text + ","
    # There are exactly two labels, Type (e.g. Type-Defect) and
    # Priority (e.g. Priority-Medium)
    for labels in issue.label:
        row += labels.text + ","
    # Status, e.g. Fixed
    row += issue.status.text + ","
    # TODO: figure out how to pull the relevant attributes out of Owner, Author
    #row += issue.owner + ","     # type gdata.projecthosting.data.Owner
    #row += issue.author[0] + "," # type atom.data.Author
    # Description
    row += "\"" + issue.content.text.encode('utf-8') + "\","
    # TODO: figure out how to retrieve the replies (they are not in issue)
    f.write(row + "\n")

f.close()
