import requests
import json
import os
from tqdm import tqdm
from sklearn.metrics import f1_score


def geoqa_send_request(question: str, url: str) -> requests.Response:
    """
    Request from GeoQA to translate a natural language question to GeoSPARQL.

    :param question: the natural language question
    :param url: the GeoQA pipeline to use
    :return: nothing useful, the generated query is written in a file # FIXME
    """

    data = {
        "question": question,
        "componentfilterinput": "",
        "componentlist": [
            "TagMeDisambiguate",
            "ConceptIdentifier",
            "PropertyIdentifier",
            "RelationDetection",
            "GeoSparqlGenerator"
        ]
    }
    headers = {"Content-Type": "application/json"}
    response = requests.post(url, data=json.dumps(data), headers=headers)
    return response


def strabon_send_request(query: str, url: str) -> requests.Response:
    """
    Execute the given query on a strabon endpoint and return the result in TSV format.

    :param query: the query to execute on a Strabon endpoint
    :param url: the Strabon endpoint to use
    :return: the result of the query
    """

    data = {
        "view": "HTML",
        "query": """PREFIX geo: <http://www.opengis.net/ont/geosparql#>\r\n
                      PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\r\n
                      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n
                      PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n
                      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\r\n
                      PREFIX yago: <http://yago-knowledge.org/resource/>\r\n
                      PREFIX y2geor: <http://kr.di.uoa.gr/yago2geo/resource/>\r\n
                      PREFIX y2geoo: <http://kr.di.uoa.gr/yago2geo/ontology/>\r\n
                      PREFIX strdf: <http://strdf.di.uoa.gr/ontology#>\r\n
                      PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>\r\n
                      PREFIX owl: <http://www.w3.org/2002/07/owl#>\r\n\r\n
                      """ + query,
        "format": "TSV",
        "handle": "download",
        "submit": "Query"
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    response = requests.post(url, data=data, headers=headers)
    return response


if __name__ == "__main__":
    if os.path.exists("results.txt"):
        os.remove("results.txt")

    with open('./resources/GeoQuestion733.json', 'r') as questions_file:
        y = []
        y_pred = []
        questions_data = json.load(questions_file)
        # for i in tqdm(questions_data):
        for i in tqdm(range(1, 150)):
            # read data from benchmark
            question = questions_data[str(i)]['Question']
            query = questions_data[str(i)]['Query']
            answer = questions_data[str(i)]['Answer']

            # request the creation of the query
            geoqa_send_request(question, "http://localhost:12345/startquestionansweringwithtextquestion")

            # request the execution of the query
            with open('query.txt', 'r') as query_file:
                predicted_query = query_file.readline()
                print(predicted_query)
                response = strabon_send_request(predicted_query, "http://pyravlos2.di.uoa.gr:8080/yago2geo/Query")

                predicted_answer = ""
                try:
                    predicted_answer = response.text.split("\n")[1].replace("\r", "")
                except ValueError:
                    predicted_answer = None

                y.append(answer)
                y_pred.append(predicted_answer)

        print("F1 Score:")
        print(f1_score(y, y_pred, average='micro'))
