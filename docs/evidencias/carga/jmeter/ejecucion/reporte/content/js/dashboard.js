/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 100.0, "KoPercent": 0.0};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.8565886622450256, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.7768184196755625, 500, 1500, "03 Consultar disponibilidad"], "isController": false}, {"data": [0.8311954848640328, 500, 1500, "02b Habitación al azar"], "isController": false}, {"data": [0.8607391537225495, 500, 1500, "05 Crear reserva"], "isController": false}, {"data": [0.9325568797399784, 500, 1500, "07 Listar reservas del día"], "isController": false}, {"data": [0.8891820580474934, 500, 1500, "08 Reporte de ocupación (30 días)"], "isController": false}, {"data": [1.0, 500, 1500, "00 Login"], "isController": false}, {"data": [0.8795090180360722, 500, 1500, "01 Panel del día"], "isController": false}, {"data": [0.8869143780290791, 500, 1500, "06 Cancelar reserva"], "isController": false}, {"data": [0.8489372469635628, 500, 1500, "02 Listar habitaciones"], "isController": false}, {"data": [0.8080808080808081, 500, 1500, "04 Registrar huésped"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 15982, 0, 0.0, 346.4911775747705, 2, 2745, 155.0, 960.0, 1217.0, 1694.17, 37.14524110008669, 614.574594915074, 17.655581431562997], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["03 Consultar disponibilidad", 1911, 0, 0.0, 580.2014652014652, 209, 2257, 350.0, 1203.6, 1445.3999999999999, 1625.8799999999999, 4.500536955743542, 62.021994578631045, 2.1228118648673155], "isController": false}, {"data": ["02b Habitación al azar", 1949, 0, 0.0, 389.6818881477686, 45, 1958, 164.0, 994.0, 1188.0, 1433.5, 4.5822596723531515, 223.29511302902293, 1.901816758545009], "isController": false}, {"data": ["05 Crear reserva", 1867, 0, 0.0, 313.0128548473488, 6, 2440, 44.0, 1007.2, 1341.1999999999998, 1891.5999999999997, 4.424369817598423, 5.01382124950531, 2.3888627390806696], "isController": false}, {"data": ["07 Listar reservas del día", 1846, 0, 0.0, 183.86511375948004, 4, 2233, 35.5, 582.3, 762.0, 1251.12, 4.395813737513246, 72.35062962114087, 1.9961458866637298], "isController": false}, {"data": ["08 Reporte de ocupación (30 días)", 379, 0, 0.0, 301.324538258575, 40, 1730, 132.0, 757.0, 900.0, 1368.3999999999987, 0.9558855055978571, 3.8450121069898184, 0.4013972337959752], "isController": false}, {"data": ["00 Login", 320, 0, 0.0, 103.45312500000001, 59, 492, 72.0, 219.60000000000014, 289.95, 411.33000000000055, 1.0267862872691333, 1.023878396416516, 0.24215711755419508], "isController": false}, {"data": ["01 Panel del día", 1996, 0, 0.0, 329.3762525050103, 63, 2123, 165.5, 797.5999999999999, 953.5999999999995, 1354.3599999999997, 4.668492586779933, 20.014338335902252, 1.9239295621300114], "isController": false}, {"data": ["06 Cancelar reserva", 1857, 0, 0.0, 261.6278944534195, 2, 2163, 25.0, 883.8000000000002, 1179.9999999999982, 1799.2800000000025, 4.408319070193638, 5.154444728652175, 2.0879245596131977], "isController": false}, {"data": ["02 Listar habitaciones", 1976, 0, 0.0, 354.6017206477736, 43, 1965, 147.5, 904.8999999999999, 1076.1499999999999, 1397.46, 4.632060610606856, 225.69600314820718, 1.9224860932694472], "isController": false}, {"data": ["04 Registrar huésped", 1881, 0, 0.0, 400.99893673577833, 4, 2745, 69.0, 1245.8, 1563.8999999999985, 2017.18, 4.449953986387541, 4.170623579817649, 3.0351892686851936], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": []}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 15982, 0, "", "", "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
