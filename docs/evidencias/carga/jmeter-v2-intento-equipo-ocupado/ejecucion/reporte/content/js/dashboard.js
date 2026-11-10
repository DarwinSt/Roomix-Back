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

    var data = {"OkPercent": 99.99122036874451, "KoPercent": 0.00877963125548727};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.6288410886742757, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.5439238653001464, 500, 1500, "03 Consultar disponibilidad"], "isController": false}, {"data": [0.6302308802308803, 500, 1500, "02b Habitación al azar"], "isController": false}, {"data": [0.608564988730278, 500, 1500, "05 Crear reserva"], "isController": false}, {"data": [0.6993769470404985, 500, 1500, "07 Listar reservas del día"], "isController": false}, {"data": [0.6096491228070176, 500, 1500, "08 Reporte de ocupación (30 días)"], "isController": false}, {"data": [0.778125, 500, 1500, "00 Login"], "isController": false}, {"data": [0.6205200281096276, 500, 1500, "01 Panel del día"], "isController": false}, {"data": [0.6200305810397554, 500, 1500, "06 Cancelar reserva"], "isController": false}, {"data": [0.6523911491791577, 500, 1500, "02 Listar habitaciones"], "isController": false}, {"data": [0.6269545793000745, 500, 1500, "04 Registrar huésped"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 11390, 1, 0.00877963125548727, 886.6636523266053, 5, 9202, 580.0, 2077.0, 2623.0, 3880.1800000000003, 26.401247974706724, 442.90774039419915, 12.660407700373652], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["03 Consultar disponibilidad", 1366, 0, 0.0, 1053.274524158126, 206, 5379, 758.5, 2150.5, 2845.2999999999997, 3883.3199999999997, 3.26949911560767, 45.03472636718797, 1.542156321131352], "isController": false}, {"data": ["02b Habitación al azar", 1386, 0, 0.0, 867.050505050504, 116, 5953, 609.5, 1946.8999999999999, 2438.899999999999, 3797.169999999999, 3.294289897510981, 160.5771930773861, 1.3672589906661785], "isController": false}, {"data": ["05 Crear reserva", 1331, 1, 0.07513148009015777, 966.5011269722022, 14, 9064, 559.0, 2459.7999999999993, 2983.199999999999, 4791.440000000002, 3.241423986050431, 4.050842951364028, 1.9114627854206545], "isController": false}, {"data": ["07 Listar reservas del día", 1284, 0, 0.0, 698.2032710280371, 12, 5714, 429.5, 1760.0, 2204.25, 3237.0000000000073, 3.144484662065858, 59.89874786939124, 1.4279153983013906], "isController": false}, {"data": ["08 Reporte de ocupación (30 días)", 228, 0, 0.0, 875.4078947368429, 119, 5148, 603.0, 1873.4, 2472.5499999999975, 4089.1000000000104, 0.5875274115036837, 2.3650273341974453, 0.2467156122525234], "isController": false}, {"data": ["00 Login", 320, 0, 0.0, 566.9250000000004, 103, 2823, 276.5, 1384.7, 1821.5, 2797.58, 1.0156763293457458, 1.0128215994283647, 0.23953694681347423], "isController": false}, {"data": ["01 Panel del día", 1423, 0, 0.0, 892.8742094167251, 183, 5089, 635.0, 1953.6000000000001, 2347.3999999999996, 3539.08, 3.327137329171489, 14.26705079335157, 1.3711444852640315], "isController": false}, {"data": ["06 Cancelar reserva", 1308, 0, 0.0, 930.6123853211004, 8, 8294, 548.0, 2340.1000000000004, 2845.3499999999995, 4336.1, 3.1952549974716447, 4.112823968443192, 1.5132082334307217], "isController": false}, {"data": ["02 Listar habitaciones", 1401, 0, 0.0, 836.9036402569602, 120, 5065, 563.0, 1896.7999999999997, 2375.0999999999976, 3567.6000000000004, 3.3085370173574216, 161.29669603849334, 1.3731721019305703], "isController": false}, {"data": ["04 Registrar huésped", 1343, 0, 0.0, 919.1183916604621, 5, 9202, 512.0, 2252.8, 2894.6, 4320.439999999999, 3.2344455201845776, 3.030561656768252, 2.2057619619392703], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["422", 1, 100.0, 0.00877963125548727], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 11390, 1, "422", 1, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["05 Crear reserva", 1331, 1, "422", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
