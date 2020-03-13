#!/usr/bin/env groovy

import groovy.json.JsonOutput;

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(auth, zoneid, serviceName,loadBalancer,findrURL,portrAuthURL) {

    print "-----------------addFindrRecord------------------------------"
    echo "Input Parameters zoneid =  ${zoneid}, serviceName = ${serviceName}, loadBalancer = ${loadBalancer}, findrURL = ${findrURL}, portrAuthURL = ${portrAuthURL} "

    //initial authentication
    def response = httpRequest httpMode: 'POST',
            url: portrAuthURL,
            customHeaders:[[name:'Authorization', value:"Basic ${auth}"]]
    def authJson = jsonParse(response.content)
    def id = ''

    //request to get the records
    response = httpRequest httpMode: 'POST',
            contentType: 'TEXT_PLAIN',
            requestBody: "{ zone( zoneId :" + '\"' + zoneid + "\" ) { records { name id } } }",
            url: findrURL,
            customHeaders:[[name:'Authorization', value:"Bearer ${authJson.token}"]]

    def recordsJson = jsonParse(response.content)
    print recordsJson
    def recordFound = false

    recordsJson.zone.records.each {
        if (serviceName == it.name) {
            recordFound = true
            id = it.id

        }
    }

    //add record if it not already present
    if (recordFound == false) {
        print "create findr record"
        def payload2 = [
                name: serviceName,
                ttl: 3600,
                type: "A",
                values: [loadBalancer]
        ]
        httpRequest httpMode: 'POST',
                requestBody: JsonOutput.toJson(payload2),
                contentType: 'APPLICATION_JSON',
                url: findrURL + 'zones/'+ zoneid + '/records',
                customHeaders: [[name:'Authorization', value:"Bearer ${authJson.token}"]]

        //request to get the records
        response = httpRequest httpMode: 'POST',
                contentType: 'TEXT_PLAIN',
                requestBody: "{ zone( zoneId :" + '\"' + zoneid + "\" ) { records { name id } } }",
                url: findrURL,
                customHeaders:[[name:'Authorization', value:"Bearer ${authJson.token}"]]

        recordsJson = jsonParse(response.content)

        recordsJson.zone.records.each {
            if (serviceName == it.name) {
                id = it.id

            }
        }

    }
    return id;
}