# Issue #14: UI/UX Implementation & Adaptive Design

## 🎯 Epic: Modern Android UI with Adaptive Intelligence
**Priority**: P1 (High)  
**Estimate**: 12-15 days  
**Dependencies**: #01 (Core Architecture), #05 (Chat Engine), #09 (Monitoring), #12 (Performance)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 14 User Interface

## 📋 Overview
Implement comprehensive user interface and user experience for iris_android using Jetpack Compose with Material Design 3. This system provides adaptive, accessible, and intelligent UI that seamlessly integrates with on-device AI capabilities while maintaining consistent performance across all Android device classes.

## 🎯 Goals
- **Modern Material Design 3**: Clean, intuitive interface following latest Material Design principles
- **Adaptive UI**: Responsive design that adapts to different screen sizes and orientations
- **Accessibility Excellence**: Full accessibility support with screen readers, high contrast, and motor assistance
- **Performance Optimization**: Smooth 60+ FPS animations and transitions across all device classes
- **Intelligence Integration**: Smart UI behaviors based on AI insights and user patterns
- **Dark/Light Theme**: Complete theming system with automatic and manual theme switching

## 📝 Detailed Tasks

### 1. Core UI Architecture

#### 1.1 Theme System Implementation
Create `app/src/main/kotlin/ui/theme/IrisTheme.kt`:

```kotlin
@Composable
fun IrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> IrisDarkColorScheme
        else -> IrisLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = IrisTypography,
        shapes = IrisShapes,
        content = content
    )
}

private val IrisLightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1BEE7),
    onSecondaryContainer = Color(0xFF4A148C),
    tertiary = Color(0xFF00BCD4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2EBF2),
    onTertiaryContainer = Color(0xFF006064),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFEBEE),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFB71C1C),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    outline = Color(0xFF9E9E9E),
    inverseOnSurface = Color(0xFFFAFAFA),
    inverseSurface = Color(0xFF2E2E2E),
    inversePrimary = Color(0xFF64B5F6),
    surfaceTint = Color(0xFF1976D2),
    outlineVariant = Color(0xFFE0E0E0),
    scrim = Color(0xFF000000)
)

private val IrisDarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFFB39DDB),
    onSecondary = Color(0xFF4A148C),
    secondaryContainer = Color(0xFF6A1B9A),
    onSecondaryContainer = Color(0xFFE1BEE7),
    tertiary = Color(0xFF4DD0E1),
    onTertiary = Color(0xFF006064),
    tertiaryContainer = Color(0xFF00838F),
    onTertiaryContainer = Color(0xFFB2EBF2),
    error = Color(0xFFEF5350),
    errorContainer = Color(0xFFC62828),
    onError = Color(0xFF000000),
    onErrorContainer = Color(0xFFFFEBEE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF757575),
    inverseOnSurface = Color(0xFF121212),
    inverseSurface = Color(0xFFE0E0E0),
    inversePrimary = Color(0xFF1976D2),
    surfaceTint = Color(0xFF64B5F6),
    outlineVariant = Color(0xFF424242),
    scrim = Color(0xFF000000)
)

val IrisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

val IrisShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)
```

#### 1.2 Main Activity Implementation
Create `app/src/main/kotlin/MainActivity.kt`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            IrisTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                IrisApp(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun IrisApp(
    viewModel: MainViewModel,
    windowSizeClass: WindowSizeClass
) {
    val navController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val useDarkTheme = isSystemInDarkTheme()
    
    // Handle system UI colors
    LaunchedEffect(useDarkTheme) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = !useDarkTheme
        )
    }
    
    // Adaptive navigation based on screen size
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactNavigation(
                navController = navController,
                viewModel = viewModel
            )
        }
        WindowWidthSizeClass.Medium -> {
            MediumNavigation(
                navController = navController,
                viewModel = viewModel
            )
        }
        WindowWidthSizeClass.Expanded -> {
            ExpandedNavigation(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun CompactNavigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = uiState.currentDestination == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (uiState.currentDestination == "chat") {
                FloatingActionButton(
                    onClick = { viewModel.startNewConversation() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("chat") {
                ChatScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("models") {
                ModelsScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { navController.navigate("chat") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MediumNavigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    // Implementation for medium screens (tablets in portrait)
    val uiState by viewModel.uiState.collectAsState()
    
    Row {
        NavigationRail {
            BottomNavItem.values().forEach { item ->
                NavigationRailItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = uiState.currentDestination == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
        
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("chat") {
                ChatScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("models") {
                ModelsScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { navController.navigate("chat") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun ExpandedNavigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    // Implementation for large screens (tablets in landscape, foldables)
    val uiState by viewModel.uiState.collectAsState()
    
    Row {
        // Permanent navigation drawer
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Iris AI",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        BottomNavItem.values().forEach { item ->
                            NavigationDrawerItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = uiState.currentDestination == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = "chat",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("chat") {
                    ChatScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("models") {
                    ModelsScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { navController.navigate("chat") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

enum class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    CHAT("Chat", Icons.Default.Chat, "chat"),
    MODELS("Models", Icons.Default.Storage, "models"),
    SETTINGS("Settings", Icons.Default.Settings, "settings")
}
```

### 2. Chat Interface Implementation

#### 2.1 Chat Screen
Create `app/src/main/kotlin/ui/chat/ChatScreen.kt`:

```kotlin
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatState = uiState.chatState
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        // Chat header
        ChatHeader(
            currentModel = chatState.currentModel,
            isGenerating = chatState.isGenerating,
            onNavigateToSettings = onNavigateToSettings,
            onClearChat = { viewModel.clearCurrentChat() }
        )
        
        // Messages list
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (chatState.messages.isEmpty()) {
                item {
                    EmptyState(
                        onSamplePromptClick = { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    )
                }
            } else {
                items(
                    items = chatState.messages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
                
                if (chatState.isGenerating) {
                    item {
                        TypingIndicator()
                    }
                }
            }
        }
        
        // Input section
        ChatInput(
            text = chatState.currentInput,
            onTextChange = viewModel::updateCurrentInput,
            onSendMessage = { viewModel.sendMessage(chatState.currentInput) },
            isEnabled = !chatState.isGenerating,
            onVoiceInput = { viewModel.startVoiceInput() },
            onImageInput = { viewModel.selectImage() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    currentModel: String?,
    isGenerating: Boolean,
    onNavigateToSettings: () -> Unit,
    onClearChat: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Iris AI",
                    style = MaterialTheme.typography.titleLarge
                )
                if (currentModel != null) {
                    Text(
                        text = currentModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            IconButton(onClick = onClearChat) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear Chat"
                )
            }
            
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Avatar(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI Avatar"
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (message.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MessageAttachments(attachments = message.attachments)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(
                imageVector = Icons.Default.Person,
                contentDescription = "User Avatar"
            )
        }
    }
}

@Composable
fun Avatar(
    imageVector: ImageVector,
    contentDescription: String?
) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.padding(6.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun EmptyState(
    onSamplePromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to Iris AI",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your intelligent assistant for conversations, analysis, and creative tasks.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val samplePrompts = listOf(
            "What can you help me with?",
            "Explain quantum computing simply",
            "Help me write a creative story",
            "Analyze this image"
        )
        
        samplePrompts.forEach { prompt ->
            SuggestionChip(
                onClick = { onSamplePromptClick(prompt) },
                label = { Text(prompt) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI Avatar"
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if ((LocalTime.current.second + index) % 3 == 0) 1f else 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "TypingDot"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = animatedAlpha),
                                shape = CircleShape
                            )
                    )
                    
                    if (index < 2) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isEnabled: Boolean,
    onVoiceInput: () -> Unit,
    onImageInput: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Iris...") },
                enabled = isEnabled,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendMessage() }
                ),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = onVoiceInput,
                            enabled = isEnabled
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Input"
                            )
                        }
                        
                        IconButton(
                            onClick = onImageInput,
                            enabled = isEnabled
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Image Input"
                            )
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                enabled = isEnabled && text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun MessageAttachments(
    attachments: List<MessageAttachment>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments) { attachment ->
            when (attachment.type) {
                AttachmentType.IMAGE -> {
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                AttachmentType.FILE -> {
                    Card(
                        modifier = Modifier.size(120.dp, 60.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = "File"
                            )
                            Text(
                                text = attachment.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data classes for chat UI
data class ChatMessage(
    val id: String,
    val content: String,
    val sender: MessageSender,
    val timestamp: Long,
    val attachments: List<MessageAttachment> = emptyList()
)

enum class MessageSender {
    USER, AI
}

data class MessageAttachment(
    val uri: String,
    val name: String,
    val type: AttachmentType
)

enum class AttachmentType {
    IMAGE, FILE
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
```

### 3. Accessibility Implementation

#### 3.1 Accessibility Support
Create `app/src/main/kotlin/ui/accessibility/AccessibilitySupport.kt`:

```kotlin
object AccessibilitySupport {
    
    @Composable
    fun AccessibilityWrapper(
        content: @Composable () -> Unit
    ) {
        val context = LocalContext.current
        val accessibilityManager = remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        }
        
        val isAccessibilityEnabled by remember {
            derivedStateOf {
                accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
            }
        }
        
        CompositionLocalProvider(
            LocalAccessibilityEnabled provides isAccessibilityEnabled,
            content = content
        )
    }
    
    @Composable
    fun AnnouncementText(
        text: String,
        modifier: Modifier = Modifier
    ) {
        val isAccessibilityEnabled = LocalAccessibilityEnabled.current
        
        if (isAccessibilityEnabled) {
            Text(
                text = text,
                modifier = modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                }
            )
        }
    }
    
    @Composable
    fun AccessibleButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        contentDescription: String? = null,
        role: Role = Role.Button,
        content: @Composable () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = modifier.semantics {
                this.role = role
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                if (!enabled) {
                    disabled()
                }
            },
            enabled = enabled
        ) {
            content()
        }
    }
    
    @Composable
    fun AccessibleTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String? = null,
        placeholder: String? = null,
        error: String? = null,
        enabled: Boolean = true
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.semantics {
                if (label != null) {
                    contentDescription = label
                }
                if (error != null) {
                    this.error(error)
                }
                if (!enabled) {
                    disabled()
                }
            },
            label = if (label != null) { { Text(label) } } else null,
            placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
            isError = error != null,
            enabled = enabled
        )
        
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Assertive
                }
            )
        }
    }
    
    @Composable
    fun ScreenReaderAnnouncement(
        message: String,
        priority: LiveRegionMode = LiveRegionMode.Polite
    ) {
        val context = LocalContext.current
        
        LaunchedEffect(message) {
            if (message.isNotEmpty()) {
                val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                if (accessibilityManager.isEnabled) {
                    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                    event.text.add(message)
                    event.className = "com.iris.android.ui.accessibility.AccessibilitySupport"
                    event.packageName = context.packageName
                    accessibilityManager.sendAccessibilityEvent(event)
                }
            }
        }
    }
    
    @Composable
    fun HighContrastSupport(
        content: @Composable () -> Unit
    ) {
        val context = LocalContext.current
        val isHighContrastEnabled = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                accessibilityManager.isHighTextContrastEnabled
            } else {
                false
            }
        }
        
        CompositionLocalProvider(
            LocalHighContrast provides isHighContrastEnabled,
            content = content
        )
    }
    
    @Composable
    fun VoiceOverSupport(
        content: @Composable () -> Unit
    ) {
        content()
    }
}

val LocalAccessibilityEnabled = compositionLocalOf { false }
val LocalHighContrast = compositionLocalOf { false }

// Accessibility-enhanced composables
@Composable
fun AccessibleCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            onClickLabel = contentDescription,
                            role = Role.Button
                        ) { onClick() }
                        .semantics {
                            if (contentDescription != null) {
                                this.contentDescription = contentDescription
                            }
                        }
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

@Composable
fun AccessibleIcon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        tint = tint
    )
}

@Composable
fun AccessibleImage(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        contentScale = contentScale
    )
}
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **UI Component Logic**
  - ViewModel state management
  - Theme system functionality
  - Navigation logic
  - Accessibility helpers

### UI Tests
- [ ] **Compose UI Testing**
  - Chat interface interactions
  - Message display and formatting
  - Input handling and validation
  - Navigation between screens

### Accessibility Tests
- [ ] **Accessibility Validation**
  - Screen reader compatibility
  - High contrast support
  - Touch target sizes
  - Keyboard navigation

### Performance Tests
- [ ] **UI Performance**
  - Animation smoothness
  - List scrolling performance
  - Memory usage optimization
  - Compose recomposition efficiency

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Modern Material Design**: Clean, intuitive interface following Material Design 3
- [ ] **Adaptive Layout**: Responsive design for phones, tablets, and foldables
- [ ] **Accessibility Excellence**: Full support for screen readers and accessibility services
- [ ] **Smooth Performance**: 60+ FPS animations across all device classes
- [ ] **Voice Integration**: Seamless voice input and text-to-speech output

### Technical Criteria
- [ ] **Animation Performance**: All animations maintain 60+ FPS
- [ ] **Accessibility Score**: 100% accessibility compliance in testing
- [ ] **Responsive Design**: Proper layout on screens from 4" to 12"+
- [ ] **Theme Support**: Complete dark/light theme implementation

### User Experience Criteria
- [ ] **Intuitive Navigation**: Easy to understand and navigate
- [ ] **Consistent Design**: Unified design language throughout app
- [ ] **Quick Interactions**: Response to user actions within 100ms
- [ ] **Error Handling**: Clear error states and recovery options

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #05 (Chat Engine), #09 (Monitoring), #12 (Performance)
- **Enables**: #15 (Testing Strategy), #16 (Deployment & Release)
- **Related**: #08 (Voice Processing), #07 (Multimodal Support)

## 📋 Definition of Done
- [ ] Complete UI implementation with Material Design 3
- [ ] Adaptive layouts for all screen sizes and orientations
- [ ] Full accessibility support with screen reader compatibility
- [ ] Smooth 60+ FPS performance across all device classes
- [ ] Complete theming system with dark/light mode support
- [ ] Voice input and text-to-speech integration
- [ ] Comprehensive UI test suite covering all interactions
- [ ] Accessibility audit completed with 100% compliance
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Documentation complete with UI guidelines and patterns
- [ ] Code review completed and approved

---

**Note**: This UI implementation provides a modern, accessible, and adaptive interface that showcases the power of on-device AI while maintaining excellent usability across all Android devices.