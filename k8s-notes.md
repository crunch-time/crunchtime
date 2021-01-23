# K8S Notes

- To create a deployment: `kubectl apply -f .\k8s-manifest.yaml`
- To expose the service locally on port 7000: `kubectl port-forward service/crunchtime-service 7000:http`
