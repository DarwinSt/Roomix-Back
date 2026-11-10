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

    var data = {"OkPercent": 99.95127892813642, "KoPercent": 0.048721071863580996};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.6804303694681283, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.6103588354773188, 500, 1500, "03 Consultar disponibilidad"], "isController": false}, {"data": [0.702675585284281, 500, 1500, "02b Habitación al azar"], "isController": false}, {"data": [0.6542995839112344, 500, 1500, "05 Crear reserva"], "isController": false}, {"data": [0.7508960573476703, 500, 1500, "07 Listar reservas del día"], "isController": false}, {"data": [0.7196078431372549, 500, 1500, "08 Reporte de ocupación (30 días)"], "isController": false}, {"data": [0.8125, 500, 1500, "00 Login"], "isController": false}, {"data": [0.6759921925829538, 500, 1500, "01 Panel del día"], "isController": false}, {"data": [0.6628975265017668, 500, 1500, "06 Cancelar reserva"], "isController": false}, {"data": [0.6926877470355731, 500, 1500, "02 Listar habitaciones"], "isController": false}, {"data": [0.660164271047228, 500, 1500, "04 Registrar huésped"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 12315, 6, 0.048721071863580996, 738.5260251725549, 5, 5137, 468.0, 1764.3999999999996, 2160.5999999999967, 3098.0, 28.6040791298228, 486.3383752849665, 13.735638168996337], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["03 Consultar disponibilidad", 1477, 0, 0.0, 906.8327691266089, 197, 4977, 686.0, 1830.6000000000001, 2262.2999999999997, 3314.7400000000007, 3.5513686113836154, 48.7586824966458, 1.6751084368147324], "isController": false}, {"data": ["02b Habitación al azar", 1495, 0, 0.0, 680.434782608696, 75, 3457, 471.0, 1524.8000000000002, 1814.4, 2622.039999999999, 3.5749300557163015, 177.50643724667614, 1.4837356188275663], "isController": false}, {"data": ["05 Crear reserva", 1442, 6, 0.4160887656033287, 795.2607489597777, 10, 5137, 394.0, 2038.8000000000002, 2448.349999999999, 3873.309999999995, 3.515058442112448, 4.3856984220934345, 2.0730400715443587], "isController": false}, {"data": ["07 Listar reservas del día", 1395, 0, 0.0, 552.7634408602154, 6, 3297, 290.0, 1468.4, 1804.8000000000002, 2573.199999999998, 3.4224310300413388, 65.19330046092418, 1.5541312782902563], "isController": false}, {"data": ["08 Reporte de ocupación (30 días)", 255, 0, 0.0, 678.7921568627453, 96, 3098, 434.0, 1539.8000000000002, 1797.3999999999999, 2493.3599999999988, 0.6600540467784186, 2.6569753718951574, 0.2771711329245312], "isController": false}, {"data": ["00 Login", 320, 0, 0.0, 464.4968750000001, 101, 2542, 262.0, 1053.1000000000004, 1215.0999999999995, 1821.0900000000036, 1.0203593578113292, 1.0175226043476875, 0.24064139151507422], "isController": false}, {"data": ["01 Panel del día", 1537, 0, 0.0, 768.0858815875071, 122, 5126, 515.0, 1689.6000000000001, 2055.1, 3033.559999999996, 3.601285872275019, 15.442623305819932, 1.4841236700195881], "isController": false}, {"data": ["06 Cancelar reserva", 1415, 0, 0.0, 780.8388692579512, 6, 4715, 437.0, 2039.000000000001, 2523.4000000000005, 3580.759999999993, 3.465232574655558, 4.460671427841124, 1.6412478503007284], "isController": false}, {"data": ["02 Listar habitaciones", 1518, 0, 0.0, 706.9090909090908, 91, 3840, 474.5, 1630.700000000001, 1959.05, 2691.959999999999, 3.59907533637603, 178.71014564563453, 1.4937568534763797], "isController": false}, {"data": ["04 Registrar huésped", 1461, 0, 0.0, 780.4113620807661, 5, 4470, 411.0, 2042.8, 2432.8999999999996, 3407.219999999992, 3.535732436291474, 3.3134611297826773, 2.411377443521454], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["422", 6, 100.0, 0.048721071863580996], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 12315, 6, "422", 6, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["05 Crear reserva", 1442, 6, "422", 6, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
