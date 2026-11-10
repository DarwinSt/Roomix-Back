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

    var data = {"OkPercent": 99.99439932791935, "KoPercent": 0.005600672080649678};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.9506860823298796, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.8981308411214953, 500, 1500, "03 Consultar disponibilidad"], "isController": false}, {"data": [0.9495159059474412, 500, 1500, "02b Habitación al azar"], "isController": false}, {"data": [0.9446555819477435, 500, 1500, "05 Crear reserva"], "isController": false}, {"data": [0.9870254685247477, 500, 1500, "07 Listar reservas del día"], "isController": false}, {"data": [0.9615384615384616, 500, 1500, "08 Reporte de ocupación (30 días)"], "isController": false}, {"data": [1.0, 500, 1500, "00 Login"], "isController": false}, {"data": [0.9668470906630582, 500, 1500, "01 Panel del día"], "isController": false}, {"data": [0.9521988527724665, 500, 1500, "06 Cancelar reserva"], "isController": false}, {"data": [0.9601003191974464, 500, 1500, "02 Listar habitaciones"], "isController": false}, {"data": [0.9375589066918002, 500, 1500, "04 Registrar huésped"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 17855, 1, 0.005600672080649678, 200.02968356202817, 3, 1387, 119.0, 498.0, 615.2000000000007, 831.8799999999974, 41.58001737255276, 697.8149008974169, 20.047120465903454], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["03 Consultar disponibilidad", 2140, 0, 0.0, 352.28738317756984, 149, 1178, 263.5, 625.0, 735.8499999999995, 876.7700000000004, 5.0700568130665316, 69.84081924359016, 2.3914428131944674], "isController": false}, {"data": ["02b Habitación al azar", 2169, 0, 0.0, 217.47487321346244, 44, 1097, 118.0, 501.0, 614.5, 755.2000000000025, 5.120650457175639, 248.5344265584165, 2.125269965136374], "isController": false}, {"data": ["05 Crear reserva", 2105, 1, 0.047505938242280284, 174.69358669833775, 7, 1387, 43.0, 518.0, 664.6999999999998, 891.7000000000003, 5.015272158925755, 6.270195386396342, 2.9577290213309886], "isController": false}, {"data": ["07 Listar reservas del día", 2081, 0, 0.0, 113.46516098029772, 4, 856, 32.0, 336.0, 421.0, 596.5399999999995, 4.993772782138564, 95.12551942226057, 2.2676800231390937], "isController": false}, {"data": ["08 Reporte de ocupación (30 días)", 416, 0, 0.0, 193.7908653846154, 38, 776, 109.0, 448.6, 534.5999999999999, 630.7499999999995, 1.0415910302987814, 4.192810768448806, 0.4373868584262461], "isController": false}, {"data": ["00 Login", 320, 0, 0.0, 80.92500000000003, 58, 349, 65.0, 112.90000000000003, 185.44999999999987, 330.9500000000001, 1.0303602074887868, 1.0274736392805508, 0.24299999034037303], "isController": false}, {"data": ["01 Panel del día", 2217, 0, 0.0, 204.8624267027514, 64, 1078, 120.0, 445.20000000000005, 540.0, 717.2800000000007, 5.194713904119218, 22.275379641589346, 2.140790300330381], "isController": false}, {"data": ["06 Cancelar reserva", 2092, 0, 0.0, 161.88718929254284, 3, 1342, 35.5, 490.70000000000005, 627.3999999999996, 929.7700000000018, 4.9958924784593925, 6.43232630646648, 2.366218605520318], "isController": false}, {"data": ["02 Listar habitaciones", 2193, 0, 0.0, 201.97902416780664, 42, 964, 114.0, 455.0, 560.0, 698.0, 5.164496149589054, 250.67090577385017, 2.1434676402103006], "isController": false}, {"data": ["04 Registrar huésped", 2122, 0, 0.0, 188.3977379830346, 3, 1350, 47.0, 548.0, 716.6999999999998, 961.77, 5.045161566425186, 4.729973634454031, 3.4418824008680438], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["422", 1, 100.0, 0.005600672080649678], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 17855, 1, "422", 1, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["05 Crear reserva", 2105, 1, "422", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
