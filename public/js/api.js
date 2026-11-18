/*--all backend calls live here---*/

var API_BASE="/api";

/* ---------- Generic request function (used by every API call) ---------- */
function apiRequest(method,url,body){
    var options ={
        method: method,
        headers: { "Accept" : "application/json"}
    };

    if(body !== undefined){
        options.headers["Content-Type"]="application/json";
        options.body = JSON.stringify(body);
    }

    return fetch(API_BASE+ url,options).then(
            function (response){
                return response.text().then(function (text){
                    var data=null;
                    if(text){
                        try{
                            data=JSON.parse(text);
                        }catch(e){
                            data=null;
                        }
                    }
                    if(!response.ok){
                        var message=(data && (data.message || data.error)) ? (data.message || data.error) : friendlyStatusMessage(response.status);
                        throw new Error(message);
                    }
                    return data;
                });
            },
            function(){
                throw new Error("Cannot reach the server.Please try again ")
            }
    );
}

function friendlyStatusMessage(status){
        if (status === 400) return "Some of the information is invalid. Please check and try again.";
        if (status === 404) return "The requested record was not found.";
        if (status === 409) return "This record conflicts with an existing one.";
        if (status >= 500) return "Something went wrong on the server. Please try again later.";
        return "Request failed.please try again";
}

    /* GET /api/equipment */
function getEquipmentList() {
        
        return apiRequest("GET","/equipment");
}

/* POST /api/equipment  (body = JSON) */
function createEquipment(data) {
  
  return apiRequest("POST", "/equipment", data);
}

/* GET /api/equipment/:id */
function getEquipmentById(id){ 
  return apiRequest("GET","/equipment/" + encodeURIComponent(id));
}

/* PUT /api/equipment/:id  (body = JSON) */
function updateEquipment(id,data){
  return apiRequest("PUT","/equipment/" + encodeURIComponent(id),data);
}

/* PATCH /api/equipment/:id/status   body: { "status": "Approved" } */
function changeEquipmentStatus(id, newStatus) {
  return apiRequest("PATCH", "/equipment/" + encodeURIComponent(id) + "/status",
                    { status: newStatus });
}

/*--shipment list--*/
/* GET /api/shipments */
function getShipmentList(){
  return apiRequest("GET","/shipments");
}

/* POST /api/shipments  (body = JSON) */
function createShipment(data) {
  return apiRequest("POST", "/shipments", data);
}

/* GET /api/shipments/:id */
function getShipmentById(id) {
  return apiRequest("GET", "/shipments/" + encodeURIComponent(id));
}

/* PUT /api/shipments/:id  (body = JSON) */
function updateShipment(id, data) {
  return apiRequest("PUT", "/shipments/" + encodeURIComponent(id), data);
}

/* PATCH /api/shipments/:id/status
   body: { "status": "Delivered", "actualDeliveryDate": "2026-11-05" }  (date is null otherwise) */
function changeShipmentStatus(id, newStatus, actualDeliveryDate) {
    return apiRequest("PATCH", "/shipments/" + encodeURIComponent(id) + "/status",
                    { status: newStatus, actualDeliveryDate: actualDeliveryDate || null });
}


/* GET /api/activity?entityType=EQUIPMENT&entityId=2&page=1&size=10
   (the backend returns newest first) */
function getActivityList() {
    return apiRequest("GET", "/activity");
}
