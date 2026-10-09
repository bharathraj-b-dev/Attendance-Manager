package com.bharath.attendancemanager.digicampus;

import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.AttendanceStatus;
import com.bharath.attendancemanager.model.SubjectAttendance;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

public class DigiCampusImporter {

    public interface ImportCallback {
        void onComplete(DigiCampusExtractionResult result);
    }

    /**
     * Proven JavaScript extraction script adapted from DigiCampusImportTest
     * with Theory/Practical component extraction and robust subject name filtering.
     */
    private static final String JS_EXTRACTION_SCRIPT =
            "(function() {" +
            "  try {" +
            "    var courses = document.querySelectorAll('.course-att-comp');" +
            "    if (courses && courses.length > 0) {" +
            "      var subjects = [];" +
            "      var errors = [];" +
            "      for (var i = 0; i < courses.length; i++) {" +
            "        var course = courses[i];" +
            "        var header = course.querySelector('.att-agg-course-header') || course;" +
            "        var nameElem = header.querySelector('.col-xs-4.ng-binding') || header.querySelector('.col-xs-4');" +
            "        var valElems = header.querySelectorAll('.col-xs-2.text-center.ng-binding');" +
            "        if (!valElems || valElems.length < 3) {" +
            "          valElems = header.querySelectorAll('.col-xs-2.text-center');" +
            "        }" +
            "        if (!nameElem || !valElems || valElems.length < 3) {" +
            "          errors.push('Row ' + (i + 1) + ': Missing elements');" +
            "          continue;" +
            "        }" +
            "        var rawName = (nameElem.textContent || '').replace(/\\s+/g, ' ').trim();" +
            "        var compMatch = rawName.match(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i);" +
            "        var compType = compMatch ? compMatch[1].trim() : '';" +
            "        if (/lecture|theory/i.test(compType)) compType = 'Theory';" +
            "        else if (/practical|lab/i.test(compType)) compType = 'Practical';" +
            "        else if (/tutorial/i.test(compType)) compType = 'Tutorial';" +
            "        var parentName = rawName.replace(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/gi, '').trim();" +
            "        var rawAtt = (valElems[0].textContent || '').replace(/[^0-9]/g, '');" +
            "        var rawSch = (valElems[1].textContent || '').replace(/[^0-9]/g, '');" +
            "        var rawPct = (valElems[2].textContent || '').replace(/[^0-9.]/g, '');" +
            "        var attended = parseInt(rawAtt, 10);" +
            "        var scheduled = parseInt(rawSch, 10);" +
            "        var percentage = parseFloat(rawPct);" +
            "        if (!parentName || isNaN(attended) || isNaN(scheduled) || isNaN(percentage)) {" +
            "          errors.push('Row ' + (i + 1) + ': Invalid data');" +
            "          continue;" +
            "        }" +
            "        subjects.push({" +
            "          name: parentName," +
            "          component: compType," +
            "          fullName: rawName," +
            "          attended: attended," +
            "          scheduled: scheduled," +
            "          percentage: percentage" +
            "        });" +
            "      }" +
            "      return JSON.stringify({" +
            "        viewType: 'main'," +
            "        count: subjects.length," +
            "        subjects: subjects," +
            "        errors: errors" +
            "      });" +
            "    }" +
            "    var present = -1;" +
            "    var absent = -1;" +
            "    var onDuty = -1;" +
            "    var statusBlocks = document.querySelectorAll('.att-agg-status');" +
            "    if (statusBlocks && statusBlocks.length > 0) {" +
            "      for (var sb = 0; sb < statusBlocks.length; sb++) {" +
            "        var block = statusBlocks[sb];" +
            "        var blockText = (block.textContent || '').replace(/\\s+/g, ' ').trim();" +
            "        var numInBlock = -1;" +
            "        var countElems = block.querySelectorAll('.att-agg-status-count, .ng-binding, b, strong, span');" +
            "        for (var ce = 0; ce < countElems.length; ce++) {" +
            "          var elemTxt = (countElems[ce].textContent || '').trim();" +
            "          if (/^\\d+$/.test(elemTxt)) {" +
            "            numInBlock = parseInt(elemTxt, 10);" +
            "            break;" +
            "          }" +
            "        }" +
            "        if (numInBlock === -1) {" +
            "          var m = blockText.match(/(\\d+)/);" +
            "          if (m) numInBlock = parseInt(m[1], 10);" +
            "        }" +
            "        if (numInBlock !== -1) {" +
            "          if (/PRESENT/i.test(blockText)) {" +
            "            present = numInBlock;" +
            "          } else if (/ABSENT/i.test(blockText)) {" +
            "            absent = numInBlock;" +
            "          } else if (/ON\\s*DUTY|OD/i.test(blockText)) {" +
            "            onDuty = numInBlock;" +
            "          }" +
            "        }" +
            "      }" +
            "    }" +
            "    if (present === -1 || absent === -1) {" +
            "      var header = document.querySelector('.overall-attendance-header');" +
            "      var summaryContainer = null;" +
            "      if (header) {" +
            "        var curr = header;" +
            "        var level = 0;" +
            "        while (curr && level < 5) {" +
            "          var currTxt = curr.textContent || '';" +
            "          if (/present/i.test(currTxt) && /absent/i.test(currTxt)) {" +
            "            summaryContainer = curr;" +
            "            break;" +
            "          }" +
            "          var sib = curr.nextElementSibling;" +
            "          while (sib) {" +
            "            var sibTxt = sib.textContent || '';" +
            "            if (/present/i.test(sibTxt) && /absent/i.test(sibTxt)) {" +
            "              summaryContainer = sib;" +
            "              break;" +
            "            }" +
            "            sib = sib.nextElementSibling;" +
            "          }" +
            "          if (summaryContainer) break;" +
            "          curr = curr.parentElement;" +
            "          level++;" +
            "        }" +
            "      }" +
            "      if (summaryContainer) {" +
            "        var containerTxt = (summaryContainer.innerText || summaryContainer.textContent || '').replace(/\\r/g, '');" +
            "        if (present === -1) {" +
            "          var presMatch = containerTxt.match(/PRESENT\\s*[:\\-]?\\s*(\\d+)/i) || containerTxt.match(/(\\d+)\\s*PRESENT/i);" +
            "          if (presMatch) present = parseInt(presMatch[1], 10);" +
            "        }" +
            "        if (absent === -1) {" +
            "          var absMatch = containerTxt.match(/ABSENT\\s*[:\\-]?\\s*(\\d+)/i) || containerTxt.match(/(\\d+)\\s*ABSENT/i);" +
            "          if (absMatch) absent = parseInt(absMatch[1], 10);" +
            "        }" +
            "      }" +
            "    }" +
            "    if (present !== -1 || absent !== -1 || (statusBlocks && statusBlocks.length > 0)) {" +
            "      var subjectName = '';" +
            "      var detectedComp = '';" +
            "      var ignoreRegex = /^(digicampus|present|absent|on\\s*duty|attendance|overall|dashboard|profile|home|academics|rajalakshmi|rec|college|university|engineering|menu|user|student|total|percentage|welcome|admin|staff|faculty|department)/i;" +
            "      var allDomElements = document.querySelectorAll('*');" +
            "      for (var d = 0; d < allDomElements.length; d++) {" +
            "        var elem = allDomElements[d];" +
            "        var eTxt = (elem.textContent || '').replace(/\\s+/g, ' ').trim();" +
            "        if (/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i.test(eTxt)) {" +
            "          var childHasMatch = false;" +
            "          for (var ch = 0; ch < elem.children.length; ch++) {" +
            "            if (/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i.test(elem.children[ch].textContent || '')) {" +
            "              childHasMatch = true;" +
            "              break;" +
            "            }" +
            "          }" +
            "          if (!childHasMatch) {" +
            "            var cm = eTxt.match(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i);" +
            "            if (cm && !detectedComp) detectedComp = cm[1].trim();" +
            "            if (!subjectName || subjectName.toLowerCase() === 'digicampus') {" +
            "              var rawBeforeBracket = eTxt.split(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i)[0];" +
            "              if (rawBeforeBracket && rawBeforeBracket.trim().length >= 3) {" +
            "                var candidate = rawBeforeBracket.trim();" +
            "                if (!ignoreRegex.test(candidate)) {" +
            "                  subjectName = candidate;" +
            "                }" +
            "              }" +
            "            }" +
            "          }" +
            "        }" +
            "      }" +
            "      if (!subjectName || subjectName.toLowerCase() === 'digicampus') {" +
            "        var titleElems = document.querySelectorAll('.course-title, .subject-title, .course-name, .selected-course, .breadcrumb .active, .sub-title, h2, h3');" +
            "        for (var ti = 0; ti < titleElems.length; ti++) {" +
            "          var tTxt = (titleElems[ti].textContent || '').replace(/\\s+/g, ' ').trim();" +
            "          if (tTxt && tTxt.length >= 3 && !ignoreRegex.test(tTxt)) {" +
            "            var cmTitle = tTxt.match(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i);" +
            "            if (cmTitle && !detectedComp) detectedComp = cmTitle[1].trim();" +
            "            var cleanT = tTxt.replace(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/gi, '').trim();" +
            "            if (cleanT.length >= 3 && !ignoreRegex.test(cleanT)) {" +
            "              subjectName = cleanT;" +
            "              break;" +
            "            }" +
            "          }" +
            "        }" +
            "      } " +
            "      if (!subjectName || subjectName.toLowerCase() === 'digicampus') {" +
            "        var headings = document.querySelectorAll('h1, h2, h3, h4, h5');" +
            "        for (var hi = 0; hi < headings.length; hi++) {" +
            "          var hTxt = (headings[hi].textContent || '').replace(/\\s+/g, ' ').trim();" +
            "          if (hTxt && hTxt.length >= 4 && !ignoreRegex.test(hTxt)) {" +
            "            var cmH = hTxt.match(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/i);" +
            "            if (cmH && !detectedComp) detectedComp = cmH[1].trim();" +
            "            var cleanH = hTxt.replace(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/gi, '').trim();" +
            "            if (cleanH.length >= 4 && !ignoreRegex.test(cleanH)) {" +
            "              subjectName = cleanH;" +
            "              break;" +
            "            }" +
            "          }" +
            "        }" +
            "      }" +
            "      if (/lecture|theory/i.test(detectedComp)) detectedComp = 'Theory';" +
            "      else if (/practical|lab/i.test(detectedComp)) detectedComp = 'Practical';" +
            "      else if (/tutorial/i.test(detectedComp)) detectedComp = 'Tutorial';" +
            "      if (subjectName) {" +
            "        subjectName = subjectName.replace(/\\[\\s*(lecture|practical|tutorial|lab|theory)\\s*\\]/gi, '').trim();" +
            "      }" +
            "      var totalSessions = -1;" +
            "      var percentage = -1;" +
            "      if (present !== -1 && absent !== -1) {" +
            "        totalSessions = present + absent;" +
            "        if (totalSessions > 0) {" +
            "          percentage = Math.round((present / totalSessions) * 10000) / 100;" +
            "        }" +
            "      }" +
            "      var ngRepeats = document.querySelectorAll('[ng-repeat]');" +
            "      var candidateElements = [];" +
            "      for (var nr = 0; nr < ngRepeats.length; nr++) {" +
            "        var nElem = ngRepeats[nr];" +
            "        if (nElem.classList.contains('course-att-comp') || nElem.querySelector('.course-att-comp')) continue;" +
            "        if (nElem.classList.contains('att-agg-status') || nElem.querySelector('.att-agg-status')) continue;" +
            "        var nTxt = (nElem.textContent || '').replace(/\\s+/g, ' ').trim();" +
            "        if (/\\b(PRESENT|ABSENT|ON\\s*DUTY|OD|P|A|UNMARKED|UPCOMING|SCHEDULED|PENDING)\\b/i.test(nTxt) ||" +
            "            /\\b(?:\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4}|\\d{1,2}:\\d{2}\\s*(?:AM|PM))\\b/i.test(nTxt)) {" +
            "          candidateElements.push(nElem);" +
            "        }" +
            "      }" +
            "      if (candidateElements.length === 0) {" +
            "        var allScope = document.querySelectorAll('.ng-scope, tr, .list-group-item, div.row, div[class*=\"col-\"]');" +
            "        for (var sc = 0; sc < allScope.length; sc++) {" +
            "          var sElem = allScope[sc];" +
            "          if (sElem.querySelector('.att-agg-status') || sElem.classList.contains('att-agg-status') || sElem.querySelector('.overall-attendance-header') || sElem.classList.contains('overall-attendance-header')) continue;" +
            "          var sTxt = (sElem.textContent || '').replace(/\\s+/g, ' ').trim();" +
            "          if (/\\b(PRESENT|ABSENT|ON\\s*DUTY|OD|P|A|UNMARKED|UPCOMING|SCHEDULED|PENDING)\\b/i.test(sTxt) ||" +
            "              /\\b(?:\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4}|\\d{1,2}:\\d{2}\\s*(?:AM|PM))\\b/i.test(sTxt)) {" +
            "            candidateElements.push(sElem);" +
            "          }" +
            "        }" +
            "      }" +
            "      var finalRecordElems = [];" +
            "      for (var m1 = 0; m1 < candidateElements.length; m1++) {" +
            "        var isParent = false;" +
            "        for (var m2 = 0; m2 < candidateElements.length; m2++) {" +
            "          if (m1 !== m2 && candidateElements[m1].contains(candidateElements[m2])) {" +
            "            isParent = true;" +
            "            break;" +
            "          }" +
            "        }" +
            "        if (!isParent) {" +
            "          finalRecordElems.push(candidateElements[m1]);" +
            "        }" +
            "      }" +
            "      var markedSessions = [];" +
            "      var unmarkedSessions = [];" +
            "      for (var k = 0; k < finalRecordElems.length; k++) {" +
            "        var recItem = finalRecordElems[k];" +
            "        var itemText = (recItem.textContent || '').replace(/\\s+/g, ' ').trim();" +
            "        var dateMatch = itemText.match(/\\b\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}\\b/i) || itemText.match(/\\b\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4}\\b/);" +
            "        var timeMatch = itemText.match(/\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM)?\\b/i);" +
            "        var statusMatch = itemText.match(/\\b(PRESENT\\s*\\([PA]\\)|ABSENT\\s*\\([PA]\\)|PRESENT|ABSENT|ON\\s*DUTY|OD)\\b/i);" +
            "        var date = dateMatch ? dateMatch[0].trim() : '';" +
            "        var time = timeMatch ? timeMatch[0].trim() : '';" +
            "        var status = '';" +
            "        if (statusMatch) {" +
            "          status = statusMatch[0].trim().toUpperCase();" +
            "        } else {" +
            "          var badges = recItem.querySelectorAll('span, div, b, strong, p, td');" +
            "          for (var bg = 0; bg < badges.length; bg++) {" +
            "            var bTxt = (badges[bg].textContent || '').trim().toUpperCase();" +
            "            if (bTxt === 'PRESENT (P)' || bTxt === 'PRESENT' || bTxt === 'P') { status = 'PRESENT (P)'; break; }" +
            "            if (bTxt === 'ABSENT (A)' || bTxt === 'ABSENT' || bTxt === 'A') { status = 'ABSENT (A)'; break; }" +
            "            if (bTxt === 'ON DUTY' || bTxt === 'ON-DUTY' || bTxt === 'OD') { status = 'ON DUTY'; break; }" +
            "          }" +
            "        }" +
            "        if (!status) {" +
            "          var html = recItem.outerHTML || '';" +
            "          if (/\\b(present|att-p|badge-success|text-success|status-p)\\b/i.test(html)) status = 'PRESENT (P)';" +
            "          else if (/\\b(absent|att-a|badge-danger|text-danger|status-a)\\b/i.test(html)) status = 'ABSENT (A)';" +
            "          else if (/\\b(onduty|att-od|badge-warning|badge-info|status-od)\\b/i.test(html)) status = 'ON DUTY';" +
            "        }" +
            "        if (status === 'P') status = 'PRESENT (P)';" +
            "        if (status === 'A') status = 'ABSENT (A)';" +
            "        if (status === 'OD') status = 'ON DUTY';" +
            "        if (status && (date || time)) {" +
            "          markedSessions.push({" +
            "            date: date," +
            "            time: time," +
            "            status: status," +
            "            rawText: itemText" +
            "          });" +
            "        } else if (date || time || /\\b(UNMARKED|UPCOMING|SCHEDULED|PENDING)\\b/i.test(itemText)) {" +
            "          unmarkedSessions.push({" +
            "            date: date," +
            "            time: time," +
            "            status: 'UNMARKED'," +
            "            rawText: itemText" +
            "          });" +
            "        }" +
            "      }" +
            "      return JSON.stringify({" +
            "        viewType: 'individual'," +
            "        subjectName: subjectName || 'Individual Subject'," +
            "        component: detectedComp," +
            "        present: present," +
            "        absent: absent," +
            "        onDuty: onDuty," +
            "        total: totalSessions," +
            "        percentage: percentage," +
            "        markedSessions: markedSessions," +
            "        unmarkedSessions: unmarkedSessions" +
            "      });" +
            "    }" +
            "    return JSON.stringify({" +
            "      viewType: 'none'," +
            "      message: 'Please open the main Attendance Records page first.'" +
            "    });" +
            "  } catch(e) {" +
            "    return JSON.stringify({ viewType: 'error', error: String(e) });" +
            "  }" +
            "})();";

    public static void extractAttendance(WebView webView, ImportCallback callback) {
        if (webView == null) {
            if (callback != null) {
                callback.onComplete(DigiCampusExtractionResult.forError("WebView is not initialized."));
            }
            return;
        }

        webView.evaluateJavascript(JS_EXTRACTION_SCRIPT, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String rawValue) {
                DigiCampusExtractionResult result = parseExtractionResult(rawValue);
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        });
    }

    /**
     * Safely parses raw evaluateJavascript() output.
     */
    public static DigiCampusExtractionResult parseExtractionResult(String rawValue) {
        JSONObject json = parseJsResult(rawValue);
        if (json == null) {
            return DigiCampusExtractionResult.forError("DigiCampus did not return a valid response. Please ensure the page is fully loaded.");
        }

        String viewType = json.optString("viewType", "none");

        if ("main".equals(viewType)) {
            JSONArray subjectsArr = json.optJSONArray("subjects");
            List<SubjectAttendance> subjectsList = new ArrayList<>();
            if (subjectsArr != null) {
                for (int i = 0; i < subjectsArr.length(); i++) {
                    JSONObject sObj = subjectsArr.optJSONObject(i);
                    if (sObj != null) {
                        String name = sObj.optString("name", "");
                        String comp = sObj.optString("component", "");
                        int attended = sObj.optInt("attended", 0);
                        int scheduled = sObj.optInt("scheduled", 0);
                        double pct = sObj.optDouble("percentage", 0.0);
                        if (!name.isEmpty() && !name.equalsIgnoreCase("REC") && !name.equalsIgnoreCase("DigiCampus")) {
                            subjectsList.add(new SubjectAttendance(name, comp, attended, Math.max(0, scheduled - attended), scheduled, pct));
                        }
                    }
                }
            }

            JSONArray errorsArr = json.optJSONArray("errors");
            List<String> errorsList = new ArrayList<>();
            if (errorsArr != null) {
                for (int j = 0; j < errorsArr.length(); j++) {
                    errorsList.add(errorsArr.optString(j));
                }
            }

            if (!subjectsList.isEmpty()) {
                return DigiCampusExtractionResult.forMain(subjectsList, errorsList);
            } else {
                return DigiCampusExtractionResult.forNotAttendancePage("No subjects found. Please open the main Attendance Records page first.");
            }

        } else if ("individual".equals(viewType)) {
            String subjectName = json.optString("subjectName", "Individual Subject");
            if (subjectName == null || subjectName.equalsIgnoreCase("DigiCampus") || subjectName.equalsIgnoreCase("REC") || subjectName.trim().isEmpty()) {
                subjectName = "Course Attendance";
            }
            String compName = json.optString("component", "");
            int present = json.optInt("present", 0);
            int absent = json.optInt("absent", 0);
            int total = json.optInt("total", present + absent);
            double percentage = json.optDouble("percentage", 0.0);

            SubjectAttendance subject = new SubjectAttendance(subjectName, compName, present, absent, total, percentage);

            JSONArray markedArr = json.optJSONArray("markedSessions");
            List<AttendanceSession> markedList = new ArrayList<>();
            if (markedArr != null) {
                for (int m = 0; m < markedArr.length(); m++) {
                    JSONObject item = markedArr.optJSONObject(m);
                    if (item != null) {
                        String d = item.optString("date", "");
                        String t = item.optString("time", "");
                        String stStr = item.optString("status", "");
                        String raw = item.optString("rawText", "");
                        AttendanceStatus st = AttendanceStatus.fromRawString(stStr);
                        markedList.add(new AttendanceSession(subjectName, compName, d, t, st, AttendanceSession.SOURCE_DIGICAMPUS, raw));
                    }
                }
            }
            subject.setMarkedSessions(markedList);

            JSONArray unmarkedArr = json.optJSONArray("unmarkedSessions");
            List<AttendanceSession> unmarkedList = new ArrayList<>();
            if (unmarkedArr != null) {
                for (int u = 0; u < unmarkedArr.length(); u++) {
                    JSONObject item = unmarkedArr.optJSONObject(u);
                    if (item != null) {
                        String d = item.optString("date", "");
                        String t = item.optString("time", "");
                        String raw = item.optString("rawText", "");
                        unmarkedList.add(new AttendanceSession(subjectName, compName, d, t, AttendanceStatus.UNMARKED, AttendanceSession.SOURCE_DIGICAMPUS, raw));
                    }
                }
            }
            subject.setUpcomingSessions(unmarkedList);

            return DigiCampusExtractionResult.forIndividual(subject);

        } else if ("error".equals(viewType)) {
            return DigiCampusExtractionResult.forError(json.optString("error", "An unknown error occurred during extraction."));
        } else {
            return DigiCampusExtractionResult.forNotAttendancePage(json.optString("message", "Please open the main Attendance Records page first."));
        }
    }

    /**
     * Safely decodes rawValue returned by evaluateJavascript().
     */
    public static JSONObject parseJsResult(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty() || rawValue.equals("null")) {
            return null;
        }

        try {
            Object tokenerResult = new JSONTokener(rawValue).nextValue();
            if (tokenerResult instanceof JSONObject) {
                return (JSONObject) tokenerResult;
            } else if (tokenerResult instanceof String) {
                String str = (String) tokenerResult;
                Object innerResult = new JSONTokener(str).nextValue();
                if (innerResult instanceof JSONObject) {
                    return (JSONObject) innerResult;
                }
            }
        } catch (Exception e) {
            try {
                return new JSONObject(rawValue);
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}