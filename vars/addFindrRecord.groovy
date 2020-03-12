#!/usr/bin/env groovy

import groovy.json.JsonOutput;

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(auth, zoneid, serviceName,loadBalancer,findrURL,portrAuthURL) {

    print "-----------------addFindrRecord------------------------------"
    //initial authentication
    def response = httpRequest httpMode: 'POST',
            url: portrAuthURL,
            customHeaders:[[name:'Authorization', value:"Basic ${auth}"]]
    def authJson = jsonParse(response.content)

    //request to get the records
    response = httpRequest httpMode: 'POST',
            contentType: 'TEXT_PLAIN',
            requestBody: "{ zone( zoneId :" + '\"' + zoneid + "\" ) { records { name } } }",
            url: findrURL,
            customHeaders:[[name:'Authorization', value:"Bearer ${authJson.token}"]]

    def recordsJson = jsonParse(response.content)
    def recordFound = false

    recordsJson.zone.records.each {
        if (serviceName == it.name) {
            recordFound = true
        }
    }

    //add record if it not already present
    if (recordFound == false) {
        def payload2 = [
                name: serviceName,
                ttl: 3600,
                type: "A",
                values: [loadBalancer]
        ]
        response = httpRequest httpMode: 'POST',
                requestBody: JsonOutput.toJson(payload2),
                contentType: 'APPLICATION_JSON',
                url: findrURL + 'zones/'+ zoneid + '/records',
                customHeaders: [[name:'Authorization', value:"Bearer ${authJson.token}"]]
        recordsJson = jsonParse(response.content)
        print recordsJson
    }
}