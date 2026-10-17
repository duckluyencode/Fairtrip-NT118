# TripFair

An Android app that recommends places to visit and generates travel itineraries within a budget and a time limit, with fair cost splitting for group trips.

**Status:** early development

## Features

- **Place discovery**: search and recommendations by area and category
- **Budget-aware itineraries**: routes that fit the available budget and time, including zero-budget trips
- **Cost transparency**: per-stop cost, running total and remaining budget, with every price labeled by its source
- **Group trips**: invite members, vote on places and split costs within each member's budget
- **Photo search**: recognize a place from a photo and suggest similar places nearby
- **SOS**: send your GPS location by SMS to saved emergency contacts

## Tech stack

| Layer | Technologies |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Google Maps SDK, Retrofit/OkHttp, Room, CameraX |
| Backend | NestJS, JWT, Docker |
| Database | MySQL on Amazon RDS |
| Infrastructure | AWS EC2, Amazon S3, GitHub Actions |
| External APIs | Google Places API (New), Google Cloud Vision API, GetYourGuide, Tiqets |

## Group itinerary planning

Group planning is a variant of the Team Orienteering Problem with per-member budget fairness, which is NP-hard. The planner chooses and orders places so that the trip fits the time limit, no member exceeds their own budget, and satisfaction is spread as evenly as possible. It works in three stages:

1. **Greedy construction** by value-to-cost ratio, with free places ranked first
2. **Local search** with 2-opt or simulated annealing
3. **Fairness check** that adjusts the plan so no member exceeds their budget

The evaluation compares plain greedy, the full pipeline and an exact ILP baseline (OR-Tools) on small instances, measuring total satisfaction, budget deviation, Jain's fairness index and runtime.
