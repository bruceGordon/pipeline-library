#!/usr/bin/env groovy

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

def call(auth,serviceName,location,org,namespace,cluster,findrZone, findrUsername, findrPassword,lab,findrRecord,endpoint,zoneName) {

    print "-----------------addCaCert------------------------------"
    echo "Input Parameters serviceName = ${serviceName}, org = ${org}, namespace = ${namespace} , cluster = ${cluster},  lab = ${lab}, findrRecord = ${findrRecord} ,endpoint = ${endpoint} "

    def dns = serviceName
    if (lab.length() > 0) {
        dns = dns + '.' + lab;
    }

    //initial authentication
    def response = httpRequest httpMode: 'POST',
            url: 'https://portr.ctl.io/portr/authenticate',
            customHeaders:[[name:'Authorization', value:"Basic ${auth}"]]
    def json = jsonParse(response.content)
    def portrToken = "${json.token}"

    //request to get the cert
    response = httpRequest httpMode: 'GET',
            contentType: 'APPLICATION_JSON',
            validResponseCodes: '200:405',
            url: 'https://fastr.ctl.io/api/orgs/' + org + '/mastrapplication/' + serviceName,
            customHeaders:[[name:'Authorization', value:"Bearer ${portrToken}"]]

    if (response.status == 404) {
        print "create ca cert"
        def payload =" { " +
                " \"metadata\": {                                                   " +
                "    \"location\": \"" + location + "\",                            " +
                "    \"organization\": \"" + org + "\",                             " +
                "    \"resource\": \"mastrapplication\",                            " +
                "    \"name\": \"" + serviceName + "\"                              " +
                " },                                                                " +
                " \"spec\": {                                                       " +
                "   \"type\": \"ca-certificate\",                                   " +
                "    \"cluster\": \"" + cluster + "\",                              " +
                "    \"namespace\": \"" + namespace + "\",                          " +
                "    \"renewalDays\": 30,                                           " +
                "    \"findrZone\":  \"" + findrZone + "\",                         " +
                "    \"findrZoneName\":\"" + zoneName + "\",                        " +
                "    \"findrUsername\": \"" + findrUsername + "\",                  " +
                "    \"findrRecord\": \"" + findrRecord + "\",                      " +
                "    \"endpoint\": \"" + endpoint + "\",                            " +
                "    \"subject\": \"" + dns + "\",                                  " +
                "    \"findrPassword\": \"" + findrPassword + "\",                  " +
                "    \"findrRecordName\": \"" + dns +"\"              " +
                "}                                                                  " +
                "}"
        print payload
        //request to get the records
        httpRequest httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: payload,
                url: 'https://fastr.ctl.io/api/orgs/' + org + '/mastrapplications',
                customHeaders:[[name:'Authorization', value:"Bearer ${portrToken}"]]
    }

}