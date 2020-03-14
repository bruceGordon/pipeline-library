#!/usr/bin/env groovy

import groovy.json.JsonOutput;

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

def getFindrRecord(zoneid,findrURL,authJson,dns) {

    def findrId = ''

    //request to get the records
    response = httpRequest httpMode: 'POST',
            contentType: 'TEXT_PLAIN',
            requestBody: "{ zone( zoneId :" + '\"' + zoneid + "\" ) { records { name id } } }",
            url: findrURL,
            customHeaders:[[name:'Authorization', value:"Bearer ${authJson.token}"]]

    def recordsJson = jsonParse(response.content)

    recordsJson.zone.records.each {
        if (dns == it.name) {
            findrId = it.id
        }
    }
    return findrId;
}

def call(auth, zoneid, serviceName,lab, loadBalancer,findrURL,portrAuthURL) {

    print "-----------------addFindrRecord------------------------------"
    echo "Input Parameters  serviceName = ${serviceName}, lab = ${lab},loadBalancer = ${loadBalancer}, findrURL = ${findrURL}, portrAuthURL = ${portrAuthURL} "

    //initial authentication
    def response = httpRequest httpMode: 'POST',
            url: portrAuthURL,
            customHeaders:[[name:'Authorization', value:"Basic ${auth}"]]
    def authJson = jsonParse(response.content)
    def dns = serviceName
    if (lab != "prod") {
        dns = dns + '.' + lab;
    }
    def findrId = getFindrRecord(zoneid,findrURL,authJson,dns)


    //add record if it not already present
    if (findrId.length() == 0) {
        print "create findr record"
        def payload2 = [
                name: dns,
                ttl: 3600,
                type: "A",
                values: [loadBalancer]
        ]
        httpRequest httpMode: 'POST',
                requestBody: JsonOutput.toJson(payload2),
                contentType: 'APPLICATION_JSON',
                url: findrURL + 'zones/'+ zoneid + '/records',
                customHeaders: [[name:'Authorization', value:"Bearer ${authJson.token}"]]

        findrId = getFindrRecord(zoneid,findrURL,authJson,dns)

    }
    return findrId;
}