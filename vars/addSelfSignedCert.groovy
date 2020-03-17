#!/usr/bin/env groovy

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

def call(auth,serviceName,location,org,namespace,cluster,lab,zoneName) {

    print "-----------------addSelfSignedCert------------------------------"

    echo "Input Parameters serviceName = ${serviceName}, location = ${location} ,namespace = ${namespace},org = ${org}, cluster = ${cluster}, lab = ${lab} "

    def dns = serviceName
    if (lab != "prod") {
        dns = dns + '.' + lab;
    }
    url = dns + "." + zoneName

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
        print "---create self signed cert---"
        //create the cert
        def payload =" { " +
                " \"metadata\": {                                                   " +
                "    \"location\": \"" + location + "\",                            " +
                "    \"organization\": \"" + org + "\",                             " +
                "    \"resource\": \"mastrapplication\",                            " +
                "    \"name\": \"" + serviceName + "\"                              " +
                " },                                                                " +
                " \"spec\": {                                                       " +
                "   \"type\": \"self-signed-certificate\",                          " +
                "    \"cluster\": \"" + cluster + "\",                              " +
                "    \"namespace\": \"" + namespace + "\",                          " +
                "    \"durationDays\": 90,                                          " +
                "    \"renewalDays\": 30,                                           " +
                "    \"subject\": \"" + url   + "\",                                " +
                "    \"subjectAlternativeNames\": [                                 " +
                "        {                                                          " +
                "            \"type\": \"DNS\",                                     " +
                "            \"value\": \"" + url + "\",                            " +
                "        }                                                          " +
                "    ]                                                              " +
                "}                                                                  " +
                "}"
        //request to get the records
        httpRequest httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: payload,
                url: 'https://fastr.ctl.io/api/orgs/' + org + '/mastrapplications',
                customHeaders:[[name:'Authorization', value:"Bearer ${portrToken}"]]
    }
}
