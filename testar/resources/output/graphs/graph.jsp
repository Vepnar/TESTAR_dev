<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Graph</title>
    <script src="js/babel.min.js"></script>
    <script src="js/cytoscape.min.js"></script>
    <script src="js/cola.min.js"></script>
    <script src="js/cytoscape-cose-bilkent.js"></script>
    <script src="js/cytoscape-cola.js"></script>
    <script src="js/cytoscape-euler.js"></script>
    <script src="js/dagre.js"></script>
    <script src="js/cytoscape-dagre.js"></script>
    <script src="js/klay.js"></script>
    <script src="js/cytoscape-klay.js"></script>
    <script src="js/jquery-3.2.1.slim.min.js"></script>
    <script src="js/jquery.magnific-popup.min.js"></script>
    <link rel="stylesheet" href="css/magnific-popup.css">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

<div class="topbar">
    <img src="img/testar-logo.png" class="logo">
    <div class="layout">
        <div class="column">
            <div><label for="layout-control">Layout: <select name="layout-control" id="layout-control">
                <option selected disabled></option>
                <option value="random">Random</option>
                <option value="grid">Grid</option>
                <option value="circle">Circle</option>
                <option value="concentric">Concentric</option>
                <option value="breadthfirst">Breadthfirst</option>
                <option value="cose">Cose</option>
                <option value="cose-bilkent">Cose-bilkent</option>
                <option value="cola">Cola</option>
                <option value="euler">Euler</option>
                <option value="dagre">Dagre</option>
                <option value="klay">Klay</option>
            </select></label></div>
            <div>
                <label for="show-labels"><input type="checkbox" id="show-labels" checked> Show node labels</label>
            </div>
            <div>
                <button id="show-all" type="button" class="skip">Show all nodes</button>
            </div>
        </div>

        <div class="column">
            <div class="extra-margin-left"><span class="legend">Legend:</span></div>
        </div>

        <div class="column">
            <div class="legend-box abstract-state"></div>
            <div class="legend-box concrete-state"></div>
            <div class="legend-box sequence-node"></div>
        </div>

        <div class="column">
            <div class="legend-text">Abstract state</div>
            <div class="legend-text">Concrete State</div>
            <div class="legend-text">Sequence Node</div>
        </div>

        <div class="column">
            <div class="legend-box first-node"></div>
            <div class="legend-box blackhole"></div>
            <div class="legend-box widget"></div>
        </div>

        <div class="column">
            <div class="legend-text">First Sequence Node</div>
            <div class="legend-text">Black hole</div>
            <div class="legend-text">Widget</div>
        </div>


    </div>
</div>

<div class="viewpane" id="cy">
</div>

<div class="cd-panel cd-panel--from-right js-cd-panel-main">
    <div class="cd-panel__container">
        <div class="panel-header" id="content-panel-header">
            <%--<button id="close-panel" type="button">Close</button>--%>
        </div>
        <div class="cd-panel__content" id="cd-content-panel">

        </div> <!-- cd-panel__content -->
    </div> <!-- cd-panel__container -->
</div>


<script>

    let cy = cytoscape({
        container: document.getElementById("cy"),

        elements: fetch("${contentFolder}/${graphContentFile}").then(function(response) {
            return response.json();
        })


        ,

        style: [ // the stylesheet for the graph
            {
                selector: 'node',
                style: {
                    'background-color': '#F6EFF7',
                    'border-width': "1px",
                    'border-color': '#000000',
                    'label': 'data(id)'
                }
            },

            {
                selector: ':parent',
                style: {
                    'background-opacity': 0.9,
                    'border-style' : 'dashed'
                }
            },

            {
                selector: 'edge',
                style: {
                    'width': 2,
                    'line-color': '#ccc',
                    'target-arrow-color': '#ccc',
                    'target-arrow-shape': 'triangle',
                    'curve-style': 'unbundled-bezier'
                }
            },
            {
                selector: '.AbstractAction',
                style: {
                    'line-color': '#1c9099',
                    'target-arrow-color': '#1c9099'
                }

            },

            {
                selector: '.AbstractState',
                style: {
                    'background-color': '#1c9099'
                }
            },

            {
                selector: '.isInitial',
                style: {
                    'background-color': '#1c9099',
                    'width': '60px',
                    'height': '60px',
                    'border-color': '#000000'
                }
            },

            {
                selector: '.ConcreteState',
                style: {
                    'background-color': '#67A9CF',
                    'background-image': function (ele) {
                        return "${contentFolder}/" + ele.data('id') + ".png"
                    },
                    'background-fit': 'contain'
                }
            },

            {
                selector: '.ConcreteAction',
                style: {
                    'line-color': '#67A9CF',
                    'target-arrow-color': '#67A9CF'
                }
            },

            {
                selector: '.isAbstractedBy',
                style: {
                    'line-color': '#bdc9e1',
                    'target-arrow-color': '#bdc9e1'
                }
            },

            {
                selector: '.SequenceStep',
                style: {
                    'line-color': '#016450',
                    'target-arrow-color': '#016450'
                }
            },

            {
                selector: '.SequenceNode',
                style: {
                    'background-color': '#016450'
                }
            },

            {
                selector: '.FirstNode',
                style: {
                    'line-color': '#014636',
                    'target-arrow-color': '#014636'
                }
            },

            {
                selector: '.TestSequence',
                style: {
                    'background-color': '#014636'
                }
            },
            {
                selector: '.Accessed',
                style: {
                    'line-color': '#d0d1e6',
                    'target-arrow-color': '#d0d1e6'
                }
            },

            {
                selector: '.Widget',
                style: {
                    'background-color': '#e7298a',
                    'background-opacity': 0.8
                }
            },

            {
                selector: '.isChildOf',
                style: {
                    'line-color': "#df65b0",
                    'target-arrow-color': '#df65b0'
                }
            },


            {
                selector: '.BlackHole',
                style: {
                    'background-color': '#000000',
                    'label': 'data(id)',
                    'background-image' : "img/blackhole-bg.jpg",
                    'background-fit': 'contain'
                }
            },

            {
                selector: '.UnvisitedAbstractAction',
                style: {
                    'line-color': "#1c9099",
                    'target-arrow-color': "#1c9099",
                    'line-style': 'dashed',
                    'width': 1
                }
            },

            {
                selector: '.no-label',
                style: {
                    'label': ''
                }
            },

            {
                selector: '.invisible',
                style: {
                    'display' : 'none'
                }
            },

            {
                selector: '.dim',
                style: {
                    'line-color': "#FFFFFF",
                    'target-arrow-color': "#FFFFFF",
                    'background-color': '#FFFFFF',
                    'border-color': '#FFFFFF',
                    'background-image-opacity': 0.05
                }
            },
            {
                selector: '.leaves',
                style: {
                    'width': '60px',
                    'height': '60px',
                    'border-width': '2px'
                }
            }
        ],

        layout: {
            name: 'grid'
        },

        wheelSensitivity: 0.5


    });

    let layoutControl = document.getElementById("layout-control");
    layoutControl.addEventListener("change", function () {
        let selectedLayout = layoutControl.value;
        cy.layout({
            name : selectedLayout,
            animate: 'end',
            animationEasing: 'ease-out',
            animationDuration: 1000
        }).run();
    });

    // highlight the leaves, which in this case will be the root of the widget tree
    cy.ready(() => cy.$(".Widget").leaves().addClass("leaves"));

    // document.getElementById("close-panel").addEventListener("click", function () {
    //     let cdPanel = document.getElementsByClassName("cd-panel")[0];
    //     cdPanel.classList.remove("cd-panel--is-visible");
    // });

    let showLabels = document.getElementById("show-labels");
    showLabels.addEventListener("change", function () {
        if (showLabels.checked) {
            cy.$('.no-label').removeClass('no-label');
        }
        else {
            cy.$('node').addClass('no-label');
        }
    });

    // when nodes get clicked, we need to open the side bar
    cy.on('tap', 'node', function(evt){
        let targetNode = evt.target;
        let sidePanel = document.getElementsByClassName("cd-panel")[0];
        let contentPanel = document.getElementById("cd-content-panel");
        let contentPanelHeader = document.getElementById("content-panel-header");

        // remove all the current child elements for both panel and panel header
        let child = contentPanel.lastChild;
        while (child) {
            contentPanel.removeChild(child);
            child = contentPanel.lastChild;
        }

        child = contentPanelHeader.lastChild;
        while (child) {
            contentPanelHeader.removeChild(child);
            child = contentPanelHeader.lastChild;
        }

        ///////////// add button section ///////////////////////////
        let closeButton = document.createElement("button");
        closeButton.id = "close-panel";
        closeButton.classList.add("skip");
        closeButton.appendChild(document.createTextNode("Close"));
        closeButton.addEventListener("click", function () {
            let cdPanel = document.getElementsByClassName("cd-panel")[0];
            cdPanel.classList.remove("cd-panel--is-visible");
        });
        contentPanelHeader.appendChild(closeButton);

        // for concrete states we provide a button to retrieve the widget tree
        if (targetNode.hasClass("ConcreteState")) {
            let form = document.createElement("form");
            let input = document.createElement("input");
            input.type = "hidden";
            input.value = targetNode.id();
            input.name = "concrete_state_id";
            form.appendChild(input);

            form.method = "POST";
            form.action = "graph";
            form.target = "_blank";
            contentPanel.appendChild(form);

            let widgetTreeButton = document.createElement("button");
            widgetTreeButton.id ="widget-tree-button";
            widgetTreeButton.classList.add("skip");
            widgetTreeButton.appendChild(document.createTextNode("Inspect widget tree"));
            widgetTreeButton.addEventListener("click", function () {
                form.submit();
            });
            contentPanelHeader.appendChild(widgetTreeButton);
        }

        // add a visibility button
        let visibilityButton = document.createElement("button");
        visibilityButton.id = "toggle-visible";
        visibilityButton.classList.add("skip");
        visibilityButton.appendChild(document.createTextNode("Make invisible"));
        visibilityButton.addEventListener("click", function () {
            targetNode.addClass("invisible");
        });
        contentPanelHeader.appendChild(visibilityButton);

        // add a highlight button
        let highlightButton = document.createElement("button");
        highlightButton.id = "highlight";
        highlightButton.classList.add("skip");
        highlightButton.appendChild(document.createTextNode("Highlight"));
        highlightButton.addEventListener("click", function () {
            cy.$("*").difference(cy.$(targetNode).closedNeighborhood()).addClass("dim");
        });
        contentPanelHeader.appendChild(highlightButton);

        //////////////// end add button section //////////////////

        /////////// screenshot segment //////////

        // create a popup anchor
        let popupAnchor = document.createElement("a");
        popupAnchor.href = "${contentFolder}/" + targetNode.id() + ".png";
        $(popupAnchor).magnificPopup(
            {type: "image"}
        );

        // add the screenshot image if the node is a concrete state
        if (targetNode.hasClass("ConcreteState")) { // add the screenshot full image
            let nodeImage = document.createElement("img");
            nodeImage.alt = "Image for node " + targetNode.id();
            nodeImage.src = "${contentFolder}/" + targetNode.id() + ".png";
            nodeImage.classList.add("node-img-full");
            popupAnchor.appendChild(nodeImage);
            contentPanel.appendChild(popupAnchor);
        }
        /////////// end screenshot segment /////////////

        ////////// data segment   //////////////
        let paragraph = document.createElement("p");
        paragraph.classList.add("paragraph-data");
        let h3 = document.createElement("h3");
        h3.appendChild(document.createTextNode("Element data:"));
        paragraph.appendChild(h3);

        // add the data into a table
        let dataTable = document.createElement("table");
        dataTable.classList.add("table-data");
        let tableHeaderRow = document.createElement("tr");
        let th1 = document.createElement("th");
        th1.appendChild(document.createTextNode("Attribute name"));
        let th2 = document.createElement("th");
        th2.appendChild(document.createTextNode("Attribute value"));
        tableHeaderRow.appendChild(th1);
        tableHeaderRow.appendChild(th2);
        let thead = document.createElement("thead");
        thead.appendChild(tableHeaderRow);
        dataTable.appendChild(thead);
        let tbody = document.createElement("tbody");

        let data = targetNode.data();
        // we want to sort the data first
        const orderedData = {};
        Object.keys(data).sort().forEach(function (key) {
            orderedData[key] = data[key];
        });

        for (let item in orderedData) {
            if (data.hasOwnProperty(item)) {
                let tr = document.createElement("tr");
                let td1 = document.createElement("td");
                td1.appendChild(document.createTextNode(item));
                let td2 = document.createElement("td");
                td2.appendChild(document.createTextNode(data[item]));
                tr.appendChild(td1);
                tr.appendChild(td2);
                tbody.appendChild(tr);
            }
        }
        dataTable.appendChild(tbody);
        paragraph.appendChild(dataTable);
        contentPanel.appendChild(paragraph);
        ////////// end data segment //////////

        sidePanel.classList.add("cd-panel--is-visible");
        // console.log( 'tapped ' + node.id() );
    });

    let showAllButton = document.getElementById("show-all");
    showAllButton.addEventListener("click", function () {
        cy.$('.invisible').removeClass('invisible');
        cy.$('.dim').removeClass('dim');
    })

</script>
</body>
</html>