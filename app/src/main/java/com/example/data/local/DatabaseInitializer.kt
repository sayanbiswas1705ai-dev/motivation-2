package com.example.data.local

import com.example.data.model.StudyDay

object DatabaseInitializer {

    private val monthThemes = mapOf(
        1 to "Foundations of Programming",
        2 to "Data Structures & Complexity",
        3 to "Object-Oriented Design",
        4 to "Relational & NoSQL Databases",
        5 to "UI/UX & Mobile Engineering",
        6 to "Client-Server Networking",
        7 to "Asynchronous Coroutines & State",
        8 to "Backend & API Engineering",
        9 to "Software Testing & QA",
        10 to "DevOps & Cloud Systems",
        11 to "AI & Machine Learning Basics",
        12 to "Advanced System Architecture"
    )

    private val subtopics = mapOf(
        1 to listOf("Variables & Constants", "Memory Allocation", "Operators & Expressions", "Control Flow (If/Else)", "Switch-Cases", "While Loops", "For Loops", "Nested Loops", "Function Parameters", "Function Return Values", "Recursion Basics", "Arrays & Vectors", "List Manipulations", "Strings & Text Processing", "Standard Libraries", "Local variable scope", "File I/O basics", "Structures", "Pointers and References", "In-depth Coding Lab"),
        2 to listOf("Big O Notation", "Time Complexity", "Space Complexity", "Singly Linked Lists", "Doubly Linked Lists", "Stack Implementation", "Queue Implementation", "Deque", "Hash Map Foundations", "Collision Resolution", "Binary Trees", "Binary Search Trees", "Tree Traversals (Inorder)", "Preorder & Postorder", "Graphs representation", "BFS Search", "DFS Search", "Dijkstra's Shortest Path", "Sorting algorithms (Bubble)", "Sorting algorithms (Merge)"),
        3 to listOf("Classes & Objects", "Encapsulation concept", "Access Modifiers", "Constructors & Destructors", "Inheritance basics", "Polymorphism (Overriding)", "Polymorphism (Overloading)", "Abstract Classes", "Interfaces & Contracts", "Composition vs Inheritance", "SOLID - Single Responsibility", "SOLID - Open/Closed", "SOLID - Liskov Substitution", "SOLID - Interface Segregation", "SOLID - Dependency Inversion", "Singleton Pattern", "Factory Pattern", "Observer Pattern", "Strategy Pattern", "OOP Coding Project"),
        4 to listOf("Relational Databases", "SQL Select Clauses", "Filters & Where", "Aggregations & Group By", "Inner Joins", "Left/Right Joins", "Subqueries", "Database Indexes", "Transactions & ACID", "Normalization (1NF/2NF/3NF)", "Foreign Key Constraints", "NoSQL Document DBs", "MongoDB Basics", "Key-Value Stores", "Redis Cache", "Database Schema Design", "Query Optimization", "Data Migrations", "Room SQLite implementation", "Database Practical Lab"),
        5 to listOf("Material 3 Design System", "Typography & Hierarchy", "Color-scheme Principles", "Jetpack Compose Scaffold", "Rows & Columns Layout", "State management with State", "State Hoisting", "LazyColumn list optimization", "Cards & Buttons custom style", "Text Fields inputs", "Animations in Compose", "Canvas custom drawings", "Adaptive viewports", "Light/Dark thematic systems", "Custom icons integration", "Bottom navigation design", "Accessibility touch targets", "Motion curves configurations", "Visual feedback ripples", "Design-to-Code challenge"),
        6 to listOf("HTTP Protocols (GET/POST)", "Status Codes meanings", "JSON Syntax & Parsers", "Retrofit network setup", "API Endpoints integration", "Query parameters", "Path coordinates", "Post form requests", "Auth Headers securely", "Interceptors & Logging", "Error handling networks", "Network timeouts", "Image upload payloads", "Binary data streams", "WebSockets real-time sync", "Caching remote results", "Offline local cache patterns", "GraphQL schemas foundations", "Mocking APIs safely", "Network Capstone Project"),
        7 to "Concurrency Foundations,Thread Allocation,Coroutines Scopes,Dispatchers (Main/IO),Suspend Functions,Jobs & Deferred results,Flow Streams,SharedFlow vs StateFlow,Flow operators (map/filter),Combining multiple Flows,Exception treatment coroutines,Channel pipes,Race conditions prevention,Mutex locks,ViewModel lifecycles,Combining Room + Flows,View states success/error,Livedata migrations,Side-effects handling,Concurrency Coding Sprint".split(","),
        8 to "Server Architecture,Web Servers (Express),Routing endpoints,Middleware functions,Request Validation,Cookies & Local Sessions,JWT Signatures,OAuth2 standard flows,Hashing database passwords,Environment env parameters,CORS policies setup,SQL client integrations,Repository design pattern,Object Relational Mappers,RESTful constraints,Rate limit parameters,API documentation (Swagger),Deployment to Cloud,Production monitoring,Server Capstone Project".split(","),
        9 to "Testing Paradigms,Unit testing assertions,AAA Pattern (Arrange/Act/Assert),JUnit framework annotations,Mocking components (Mockito),Testing suspend functions,Testing flows streams,State flow validations,Robolectric local JVM tests,Integration testing boundaries,UI Automated testing,Espresso selectors,Roborazzi screenshot setups,Visual regression verification,Coverage parameters check,Test Driven Development (TDD),CI automation pipelines,Stubbing network responses,Mocking Room database,Testing Capstone Project".split(","),
        10 to "Linux systems basics,Bash Scripting skills,Git branching strategy,Conflicts resolutions,CI/CD basics,GitHub Actions workflow,Docker container basics,Dockerfile writing,Docker Compose multi-containers,AWS Cloud Providers,GCP Cloud Providers,Terraform IaC basics,Serverless configurations,Load balancers,Reverse Proxy (Nginx),Virtual private clouds,Elastic Container Registry,Kubernetes basics,Log aggregations,Cloud DevOps Sprint".split(","),
        11 to "Machine learning definition,Supervised vs Unsupervised,Linear Regression algorithms,Logistic Regression,Cost Functions gradient descent,Feature Engineering,Training vs Validation sets,Overfitting mitigation,Decision Trees,Random Forests classifiers,Unsupervised Clustering,K-Means algorithms,Neural Networks basics,Weights & Biases,Deep Learning activation functions,CNN Image recognition,RNN Sequential patterns,Transformer structures,Large Language Models,ML Model Deployment Lab".split(","),
        12 to "Highly scalable systems,Monolith vs Microservices,Database Sharding,Master-Slave Replications,Event-Driven Architectures,Message Queues (Kafka),CDN assets caching,System design of Whatsapp,System design of Netflix,System design of Twitter,System design of Uber,System design of Spotify,System design of Instagram,Rate-limiters architectures,Distributed ID generators,DNS & load distribution,Consistent Hashing,CAP Theorem,SQL vs NoSQL tradeoffs,Capstone Final Presentation".split(",")
    )

    fun generateDays(): List<StudyDay> {
        val list = mutableListOf<StudyDay>()
        var globalDayId = 1
        for (m in 1..12) {
            val theme = monthThemes[m] ?: "Study Block"
            val topics = subtopics[m] ?: List(20) { "Subtopic $it" }

            for (d in 1..30) {
                val title: String
                val description: String
                val duration: Int

                when (d) {
                    1 -> {
                        title = "Introducing $theme"
                        description = "Welcome to Month $m. Set up your learning objectives, review the syllabus, and prepare your coding workspace for the deep dives ahead."
                        duration = 20
                    }
                    10 -> {
                        title = "Milestone Core Assessment"
                        description = "Take a specialized review checkpoint. Assess the fundamental definitions, syntax, and conceptual architectures covered in days 1 to 9."
                        duration = 45
                    }
                    20 -> {
                        title = "Comprehensive Hands-On Lab"
                        description = "Practice makes permanent. Build a robust local utility utilizing the theories, design choices, and tools mastered since the beginning of this month."
                        duration = 60
                    }
                    30 -> {
                        title = "Month $m Capstone Presentation"
                        description = "Your qualification exam. Compile all sub-modules, clean the code styling, document your learnings, and unlock the next monthly planner block!"
                        duration = 90
                    }
                    else -> {
                        // Map days 2-9 to index 0-7, days 11-19 to index 8-16, days 21-29 to index 17..
                        val index = when {
                            d < 10 -> d - 2
                            d < 20 -> d - 3
                            else -> d - 4
                        }
                        val topicName = if (topics.size > index && index >= 0) topics[index] else "Advanced Integration Guide"
                        title = topicName
                        description = "Dive deep into $topicName. Analyze practical case studies, review optimal implementations, and execute writing code samples on your device."
                        // Generates standard intervals: 25, 40, or 50 minutes
                        duration = when (d % 3) {
                            0 -> 30
                            1 -> 45
                            else -> 60
                        }
                    }
                }

                list.add(
                    StudyDay(
                        dayId = globalDayId,
                        month = m,
                        dayIndex = d,
                        title = title,
                        description = description,
                        durationMinutes = duration,
                        isCompleted = false,
                        completionTimestamp = null
                    )
                )
                globalDayId++
            }
        }
        return list
    }
}
