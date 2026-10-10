import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/ghost_bridge.dart';
import 'installed_apps_sheet.dart';
import 'futuristic_shell.dart';
import 'ghost_home.dart';
import 'ghost_settings.dart';
import 'ghost_history.dart';
import 'listening_consent_dialog.dart';
import 'personalization_dialog.dart';
import 'emergency_setup_dialog.dart';

class AssistantScreen extends StatefulWidget {
  const AssistantScreen({super.key});

  @override
  State<AssistantScreen> createState() => _AssistantScreenState();
}

class _AssistantScreenState extends State<AssistantScreen> {
  final _bridge = GhostBridge();
  final _command = TextEditingController();
  StreamSubscription<Map<String, dynamic>>? _subscription;
  Map<String, dynamic> _state = {};
  bool _busy = false;
  String? _error;
  int _tab = 0;
  String? _lastScreenRequest;

  bool get _listening => _state['listening'] == true;
  Map<String, dynamic>? get _pending => _state['pending'] is Map
      ? Map<String, dynamic>.from(_state['pending'] as Map)
      : null;

  @override
  void initState() {
    super.initState();
    _command.addListener(_refreshCommand);
    _subscription = _bridge.snapshots.listen(
      (state) {
        if (mounted) {
          setState(() {
            _state = state;
            final request = state['screenRequest'];
            if (request is Map && request['id'] != _lastScreenRequest) {
              _lastScreenRequest = request['id'] as String?;
              _tab = request['screen'] == 'settings' ? 1 : 0;
            }
          });
        }
      },
      onError: (Object error) {
        if (mounted) {
          setState(() => _error = 'Android connection unavailable: $error');
        }
      },
    );
  }

  void _refreshCommand() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _command.removeListener(_refreshCommand);
    _command.dispose();
    super.dispose();
  }

  Future<void> _act(String method, [Map<String, Object?>? arguments]) async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await _bridge.invoke(method, arguments);
    } on PlatformException catch (error) {
      if (mounted) setState(() => _error = error.message ?? error.code);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _start() async {
    final consent = await showDialog<bool>(
      context: context,
      builder: (context) => const ListeningConsentDialog(),
    );
    if (consent == true && mounted) await _act('startListening');
  }

  void _submit() {
    final text = _command.text.trim();
    if (text.isEmpty || _busy) return;
    _command.clear();
    _act('submit', {'text': text});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      body: CustomPaint(
        painter: GhostBackdrop(),
        child: SafeArea(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 125),
            children: [
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: _panel([
                    Text(
                      _error!,
                      style: const TextStyle(color: Colors.orangeAccent),
                    ),
                    TextButton(
                      onPressed: () => setState(() => _error = null),
                      child: const Text('Dismiss'),
                    ),
                  ]),
                ),
              if (_busy) const LinearProgressIndicator(),
              if (_pending != null) ...[
                _approval(_pending!),
                const SizedBox(height: 16),
              ],
              if ((_state['contactChoices'] as List? ?? []).isNotEmpty) ...[
                _contactChoices(),
                const SizedBox(height: 16),
              ],
              if (_tab == 0) ..._assistant(),
              if (_tab == 1) ..._settings(),
              if (_tab == 2) ..._activity(),
            ],
          ),
        ),
      ),
      bottomNavigationBar: GhostNavigation(
        selected: _tab,
        onSelected: (value) => setState(() => _tab = value),
      ),
    );
  }

  Widget _panel(List<Widget> children) => Container(
    decoration: BoxDecoration(
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xED121E35), Color(0xEF101126)],
      ),
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: ghostCyan.withValues(alpha: .2)),
      boxShadow: [
        BoxShadow(color: ghostPurple.withValues(alpha: .06), blurRadius: 28),
      ],
    ),
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: children,
      ),
    ),
  );

  Widget _contactChoices() => _panel([
    const Text('Who did you mean, boss?', style: TextStyle(fontSize: 21)),
    const SizedBox(height: 10),
    const Text('Say the full name or tap a match.'),
    for (final item in _state['contactChoices'] as List)
      ListTile(
        leading: const Icon(Icons.person_outline, color: ghostCyan),
        title: Text((item as Map)['name'] as String),
        subtitle: Text(item['phone'] as String),
        onTap: _busy ? null : () => _act('chooseContact', {'id': item['id']}),
      ),
    TextButton(
      onPressed: () => _act('cancel'),
      child: const Text('Cancel message'),
    ),
  ]);

  List<Widget> _assistant() => [
    GhostHome(
      name: _state['assistantName'] as String? ?? 'Ghost',
      state: _state,
      command: _command,
      busy: _busy,
      onSubmit: _submit,
      onMicrophone: _listening ? () => _act('speakNow') : _start,
      onListening: _listening ? () => _act('stopListening') : _start,
      onQuickCommand: (text) => _act('submit', {'text': text}),
    ),
  ];

  Future<void> _rename() async {
    final name = await showDialog<String>(
      context: context,
      builder: (context) => PersonalizationDialog(
        initialName: _state['assistantName'] as String? ?? 'Ghost',
      ),
    );
    if (name != null && mounted) await _act('setAssistantName', {'name': name});
  }

  Future<void> _configureEmergency() async {
    final setup = await showDialog<EmergencySetup>(
      context: context,
      builder: (context) => const EmergencySetupDialog(),
    );
    if (setup != null && mounted) {
      await _act('configureEmergency', {
        'phrase': setup.phrase,
        'remotePhrase': setup.remotePhrase,
        'stopPhrase': setup.stopPhrase,
        'phone': setup.phone,
        'message': setup.message,
      });
    }
  }

  Widget _approval(Map<String, dynamic> pending) => _panel([
    const Row(
      children: [
        Icon(Icons.shield_outlined, color: Color(0xFF48DEFF)),
        SizedBox(width: 10),
        Text('GHOST ACTION', style: TextStyle(letterSpacing: 2)),
      ],
    ),
    const SizedBox(height: 16),
    Text(pending['title'] as String, style: const TextStyle(fontSize: 21)),
    const SizedBox(height: 12),
    Text(pending['detail'] as String, style: const TextStyle(height: 1.5)),
    if (pending['automatic'] == true) ...[
      const SizedBox(height: 12),
      const Text(
        'This continues automatically when unlocked and launching is allowed. You can cancel while it waits.',
      ),
    ],
    const SizedBox(height: 20),
    FilledButton(
      onPressed: _busy ? null : () => _act('approve', {'id': pending['id']}),
      child: Text(
        pending['kind'] == 'message' ? 'Open draft for review' : 'Continue now',
      ),
    ),
    TextButton(
      onPressed: _busy ? null : () => _act('cancel'),
      child: const Text('Cancel action'),
    ),
  ]);

  List<Widget> _settings() => [
    GhostSettings(
      state: _state,
      busy: _busy,
      act: _act,
      rename: _rename,
      configureEmergency: _configureEmergency,
      browse: () => _browseApps(
        (_state['apps'] as List? ?? [])
            .map((app) => Map<String, dynamic>.from(app as Map))
            .toList(),
      ),
    ),
  ];

  Future<void> _browseApps(List<Map<String, dynamic>> apps) async {
    final package = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      builder: (context) => InstalledAppsSheet(apps: apps),
    );
    if (package != null && mounted) {
      await _act('openPackage', {'package': package});
    }
  }

  List<Widget> _activity() => [
    GhostHistory(
      history: _state['history'] as List? ?? [],
      onClear: () => _act('clearHistory'),
    ),
  ];
}
