curl -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjM4MTFiMDdmMjhiODQxZjRiNDllNDgyNTg1ZmQ2NmQ1NWUzOGRiNWQiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vZXhwZW5zZS10cmFja2VyLWdjcC00ODEwMDQiLCJhdWQiOiJleHBlbnNlLXRyYWNrZXItZ2NwLTQ4MTAwNCIsImF1dGhfdGltZSI6MTc2NTk0MTI2MCwidXNlcl9pZCI6ImhjdEcwS0VUUlZZYnloUUlNaUZLOERxV2NLVjIiLCJzdWIiOiJoY3RHMEtFVFJWWWJ5aFFJTWlGSzhEcVdjS1YyIiwiaWF0IjoxNzY1OTQxMjYwLCJleHAiOjE3NjU5NDQ4NjAsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6eyJlbWFpbCI6WyJ0ZXN0QGV4YW1wbGUuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoicGFzc3dvcmQifX0.cyzdmrJ1hxoJXafsj5CpzrJeo4sV_8yx4wTE8J4AC6ryIRuoD-9dmdFecTaX3607AkHPdk3DWHO4LsHLMAlIrBUbisDZx3onkHP4E47bglIJiVwZVOdCSYmVZZni-wf-ZXHkgfDgeUbLPsFI3qyicfnhCGC5-fc8jKd8Y95fZ3Gx-zhdVe0wcTNdOzgEUbViEJFmkvzRwBarYm4BaJmkVo3tkfrbxfaT1vV-de7YN8qlqcPNiSKTEFJNn3LFe05eBT-UjMdCrl3eIvi12a7QvCUWxsN5ZILvRmaA72ZDaFOL0nqIQBZFj1SsffHg8QdPf4mYwmgPhnyDW6YafkKvdQ' \
  -d '{
        "householdId": 1,
        "categoryId": 101,
        "originalAmount": 150.50,
        "originalCurrency": "USD",
        "transactionDate": "2025-12-14",
        "description": "Success via curl"
      }'