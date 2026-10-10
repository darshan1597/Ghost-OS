import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import 'ghost_home.dart';
import 'futuristic_shell.dart';

typedef SettingsAction = Future<void> Function(
  String method, [
  Map<String, Object?>? arguments,
]);

class GhostSettings extends StatelessWidget {
  const GhostSettings({
    super.key,
    required this.state,
    required this.busy,
    required this.act,
    required this.rename,
    required this.configureEmergency,
    required this.browse,
  });
  final Map<String, dynamic> state;
  final bool busy;
  final SettingsAction act;
  final VoidCallback rename, configureEmergency, browse;
  static const muted = Color(0xFFADBEDC);
  Widget copy(String text) => Text(
    text,
    style: const TextStyle(fontSize: 12, height: 1.4, color: muted),
  );
  Widget button(
    String text,
    String method, {
    IconData icon = Icons.chevron_right,
    bool filled = false,
    bool disabled = false,
  }) {
    final callback = busy || disabled ? null : () => act(method);
    final child = Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 17),
        const SizedBox(width: 7),
        Flexible(child: Text(text, textAlign: TextAlign.center)),
      ],
    );
    return filled
        ? Container(
            decoration: BoxDecoration(
              gradient: neonGradient,
              borderRadius: BorderRadius.circular(13),
            ),
            child: TextButton(
              onPressed: callback,
              style: TextButton.styleFrom(foregroundColor: Colors.white),
              child: child,
            ),
          )
        : OutlinedButton(
            onPressed: callback,
            style: OutlinedButton.styleFrom(
              foregroundColor: const Color(0xFF00CBFF),
              side: const BorderSide(color: Color(0xFF008CCE)),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(13),
              ),
            ),
            child: child,
          );
  }

  Widget section(
    String title,
    IconData icon,
    List<Widget> children, {
    Widget? trailing,
    Color? badgeColor,
  }) => Padding(
    padding: const EdgeInsets.only(bottom: 10),
    child: ClipRRect(
      borderRadius: BorderRadius.circular(19),
      child: BackdropFilter(
        filter: ui.ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(19),
            gradient: const LinearGradient(
              colors: [Color(0xB307182C), Color(0xAB080F20)],
            ),
            border: Border.all(color: const Color(0xFF19365E)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 39,
                    height: 39,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(11),
                      gradient: LinearGradient(
                        colors: badgeColor == null
                            ? [const Color(0xFF003999), const Color(0xFF290B62)]
                            : [badgeColor, badgeColor],
                      ),
                      border: Border.all(color: const Color(0xFF3434AC)),
                    ),
                    child: Icon(icon, color: const Color(0xFF49D8FF), size: 25),
                  ),
                  const SizedBox(width: 11),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: Text(
                        title,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ),
                  ?trailing,
                ],
              ),
              const SizedBox(height: 10),
              ...children,
            ],
          ),
        ),
      ),
    ),
  );
  Widget choices(
    List<(String, String)> values,
    String selected,
    String method,
    String argument,
  ) => SegmentedButton<String>(
    showSelectedIcon: false,
    style: ButtonStyle(
      textStyle: const WidgetStatePropertyAll(TextStyle(fontSize: 11)),
      visualDensity: VisualDensity.compact,
      foregroundColor: WidgetStateProperty.resolveWith(
        (s) => s.contains(WidgetState.selected) ? Colors.white : muted,
      ),
      backgroundColor: WidgetStateProperty.resolveWith(
        (s) => s.contains(WidgetState.selected)
            ? const Color(0xFF5532DD)
            : Colors.transparent,
      ),
      side: const WidgetStatePropertyAll(BorderSide(color: Color(0xFF3477BC))),
    ),
    segments: values
        .map((v) => ButtonSegment(value: v.$1, label: Text(v.$2)))
        .toList(),
    selected: {selected},
    onSelectionChanged: busy ? null : (v) => act(method, {argument: v.first}),
  );

  String reminderTime(num value) {
    final date = DateTime.fromMillisecondsSinceEpoch(value.toInt());
    final hour = date.hour % 12 == 0 ? 12 : date.hour % 12;
    final minute = date.minute.toString().padLeft(2, '0');
    final period = date.hour < 12 ? 'AM' : 'PM';
    return '${date.day}/${date.month}/${date.year} · $hour:$minute $period';
  }

  String _bluetoothStatus(Map<String, dynamic> state) {
    final audio = Map<String, dynamic>.from(
      state['bluetoothAudio'] as Map? ?? {},
    );
    if (audio['permission'] != true) {
      return 'Allow Nearby devices so Ghost can use your headset microphone.';
    }
    if (audio['routed'] == true) {
      return 'Using ${audio['name'] ?? 'Bluetooth audio'} while listening.';
    }
    if (audio['available'] == true) {
      return 'A communication headset is available and will be used in driving mode.';
    }
    return 'Connect a Bluetooth headset or car system, then enable driving mode.';
  }

  @override
  Widget build(BuildContext context) {
    final link = Map<String, dynamic>.from(state['link'] as Map? ?? {});
    final peers = (link['devices'] as List? ?? []).cast<Map>();
    final apps = (state['apps'] as List? ?? []);
    final mode = state['voiceMode'] as String? ?? 'brief';
    final voices = (state['ttsVoices'] as List? ?? []).cast<Map>();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: 8),
        Row(
          children: [
            const Expanded(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Ghost',
                      style: TextStyle(
                        fontSize: 29,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    GradientText(
                      'OS',
                      style: TextStyle(
                        fontSize: 29,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xAA07172B),
                border: Border.all(color: const Color(0xFF15355B)),
                borderRadius: BorderRadius.circular(13),
              ),
              child: const Row(
                children: [
                  Icon(Icons.phone_android, color: Color(0xFF247FFF), size: 15),
                  SizedBox(width: 6),
                  Icon(Icons.circle, color: Color(0xFF00D987), size: 7),
                  SizedBox(width: 5),
                  Text(
                    'On your phone',
                    style: TextStyle(fontSize: 10, color: Colors.white),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 5),
        const Text('Settings', style: TextStyle(fontSize: 15, color: muted)),
        const SizedBox(height: 14),
        section('Ghost Link', Icons.link, [
          copy(link['status'] as String? ?? 'Not connected'),
          const SizedBox(height: 5),
          copy(
            'Install Ghost Link on your other Android phone. Pair both phones in Bluetooth settings. On that phone, approve this Vivo and enable receiving.',
          ),
          const SizedBox(height: 9),
          Wrap(
            spacing: 8,
            runSpacing: 5,
            children: [
              button(
                'Bluetooth settings',
                'linkBluetoothSettings',
                icon: Icons.bluetooth,
              ),
              button(
                'Allow and refresh paired phones',
                'linkRefresh',
                icon: Icons.refresh,
                filled: true,
              ),
            ],
          ),
          if (peers.isNotEmpty)
            Container(
              margin: const EdgeInsets.only(top: 10),
              decoration: BoxDecoration(
                color: const Color(0x66031123),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFF193255)),
              ),
              child: Column(
                children: [
                  for (final peer in peers)
                    ListTile(
                      dense: true,
                      leading: const Icon(Icons.phone_android, color: muted),
                      title: Text(
                        peer['name'] as String? ?? 'Paired phone',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.white,
                        ),
                      ),
                      subtitle: Text(
                        peer['address'] as String? ?? '',
                        style: const TextStyle(fontSize: 10, color: muted),
                      ),
                      trailing: TextButton(
                        onPressed: busy
                            ? null
                            : () => act('linkConnect', {
                                'address': peer['address'],
                              }),
                        child: const Text('Connect'),
                      ),
                    ),
                ],
              ),
            ),
          Align(
            alignment: Alignment.centerRight,
            child: button('Disconnect', 'linkDisconnect'),
          ),
          copy(
            'Say “open Chrome on my other phone”. Opens apps only. The other phone must be unlocked with Ghost Link visible or its connection bubble enabled.',
          ),
        ]),
        section(
          'Floating assistant bubble',
          Icons.sensors,
          [
            copy(
              'Optional visible Ghost bubble while listening. Allows supported app and alarm requests while Ghost is in the background.',
            ),
            const SizedBox(height: 10),
            DropdownButtonFormField<int>(
              key: ValueKey(state['bubbleSize'] ?? 48),
              initialValue: state['bubbleSize'] as int? ?? 48,
              decoration: const InputDecoration(labelText: 'Bubble size'),
              items: const [
                DropdownMenuItem(value: 40, child: Text('Small (40 dp)')),
                DropdownMenuItem(value: 48, child: Text('Medium (48 dp)')),
                DropdownMenuItem(value: 56, child: Text('Large (56 dp)')),
              ],
              onChanged: busy
                  ? null
                  : (size) {
                      if (size != null) act('setBubbleSize', {'size': size});
                    },
            ),
            SwitchListTile(
              key: const ValueKey('bubbleIdleFade'),
              contentPadding: EdgeInsets.zero,
              title: const Text(
                'Fade when idle',
                style: TextStyle(fontSize: 13),
              ),
              subtitle: const Text(
                'Same size, 45% opacity after 15 seconds. Tap to brighten.',
                style: TextStyle(fontSize: 11, color: muted),
              ),
              value: state['bubbleFade'] != false,
              onChanged: busy
                  ? null
                  : (value) => act('setBubbleFade', {'enabled': value}),
            ),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(11),
              decoration: BoxDecoration(
                color: const Color(0x550F1D37),
                borderRadius: BorderRadius.circular(13),
              ),
              child: Column(
                children: [
                  if (state['handsFree'] == true)
                    copy(
                      state['overlayAllowed'] == true
                          ? 'Permission granted. Enable listening to show the bubble. Tap or hold for quick controls; drag to move it.'
                          : 'Grant “Display over other apps” in Android settings. Toggle this option again to reopen the permission screen.',
                    ),
                  const SizedBox(height: 7),
                  copy(
                    'Locked phone: alarms wait for unlock. “Unlock phone” requests Android authentication; you may need to tap its notification. Ghost never enters your PIN.',
                  ),
                ],
              ),
            ),
          ],
          trailing: Switch(
            key: const ValueKey('assistantBubbleEnabled'),
            value: state['handsFree'] == true,
            activeTrackColor: const Color(0xFF007BFF),
            activeThumbColor: Colors.white,
            onChanged: busy ? null : (v) => act('setHandsFree', {'enabled': v}),
          ),
        ),
        section('Hands-free intelligence', Icons.graphic_eq_outlined, [
          copy(
            'Tune wake detection and natural turn-taking. Recognition stays on this phone.',
          ),
          const SizedBox(height: 10),
          const Text('Wake-word sensitivity', style: TextStyle(fontSize: 13)),
          const SizedBox(height: 6),
          choices(
            const [
              ('strict', 'Strict'),
              ('balanced', 'Balanced'),
              ('high', 'High'),
            ],
            state['wakeSensitivity'] as String? ?? 'balanced',
            'setWakeSensitivity',
            'value',
          ),
          const SizedBox(height: 10),
          copy(
            'High hears more variants but may activate more often. Strict requires a clearer wake phrase. The phrase is “Hey ${state['assistantName'] ?? 'Ghost'}”; “Hey Ghost” remains a fallback.',
          ),
          const SizedBox(height: 12),
          const Text('End-of-speech pause', style: TextStyle(fontSize: 13)),
          const SizedBox(height: 6),
          choices(
            const [
              ('fast', 'Fast'),
              ('balanced', 'Balanced'),
              ('noisy', 'Noisy room'),
            ],
            state['speechPause'] as String? ?? 'balanced',
            'setSpeechPause',
            'value',
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            dense: true,
            title: const Text(
              'Conversational follow-ups',
              style: TextStyle(fontSize: 13),
            ),
            subtitle: copy(
              'After a conversational reply, Ghost listens for your next sentence without another wake phrase.',
            ),
            value: state['conversationFollowUps'] != false,
            onChanged: busy
                ? null
                : (v) => act('setConversationFollowUps', {'enabled': v}),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            dense: true,
            title: const Text(
              'Voice interruption',
              style: TextStyle(fontSize: 13),
            ),
            subtitle: copy(
              'Say “Ghost stop”, “stop talking”, or “be quiet” while Ghost is replying.',
            ),
            value: state['voiceInterruption'] != false,
            onChanged: busy
                ? null
                : (v) => act('setVoiceInterruption', {'enabled': v}),
          ),
          copy(
            'Battery saver: while waiting for the wake phrase, Ghost now keeps the full command decoder asleep and runs only the small wake grammar.',
          ),
        ]),
        section('Installed apps · ${apps.length}', Icons.apps, [
          copy(
            'Scanned locally when Ghost opens and before app commands. Say “open” and the app’s name. Hidden apps and other profiles may not be visible.',
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 5,
            children: [
              OutlinedButton.icon(
                onPressed: busy ? null : browse,
                icon: const Icon(Icons.apps, size: 17),
                label: const Text('Browse installed apps'),
              ),
              button(
                'Rescan apps',
                'scanApps',
                icon: Icons.refresh,
                filled: true,
              ),
            ],
          ),
        ]),
        section('Spoken responses', Icons.volume_up_outlined, [
          copy(state['voiceStatus'] as String? ?? 'Checking voice…'),
          copy(
            'Brief keeps replies short. Full speaks detailed results. Off silences the greeting and replies.',
          ),
          const SizedBox(height: 10),
          choices(
            [('off', 'Off'), ('brief', 'Brief'), ('full', 'Full')],
            mode,
            'setVoiceMode',
            'mode',
          ),
        ]),
        section('Voice character', Icons.mic_none, [
          DropdownButtonFormField<String>(
            key: ValueKey('preset-${state['voicePreset'] ?? 'cinematic'}'),
            initialValue: state['voicePreset'] as String? ?? 'cinematic',
            isExpanded: true,
            decoration: const InputDecoration(labelText: 'Voice engine'),
            items: const [
              DropdownMenuItem(
                value: 'cinematic',
                child: Text(
                  'Ghost Cinematic Monster Male',
                  style: TextStyle(fontSize: 12),
                ),
              ),
              DropdownMenuItem(
                value: 'android',
                child: Text(
                  'Android offline TTS',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
            onChanged: busy
                ? null
                : (preset) {
                    if (preset != null) {
                      act('setVoicePreset', {'preset': preset});
                    }
                  },
          ),
          const SizedBox(height: 10),
          choices(
            [('man', 'Monster man'), ('woman', 'Monster woman')],
            state['voiceCharacter'] as String? ?? 'man',
            'setVoiceCharacter',
            'character',
          ),
          const SizedBox(height: 9),
          copy(
            'Cinematic Male uses the bundled offline neural voice. Monster woman uses Android offline TTS. The installed voice below also provides the fallback.',
          ),
          if (voices.isNotEmpty) ...[
            const SizedBox(height: 10),
            DropdownButtonFormField<String>(
              key: ValueKey(state['ttsVoice']),
              initialValue: state['ttsVoice'] as String?,
              isExpanded: true,
              decoration: const InputDecoration(
                labelText: 'Android offline voice / fallback',
              ),
              items: voices
                  .map(
                    (v) => DropdownMenuItem(
                      value: v['id'] as String,
                      child: Text(
                        v['label'] as String,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 12),
                      ),
                    ),
                  )
                  .toList(),
              onChanged: busy
                  ? null
                  : (id) {
                      if (id != null) act('selectVoice', {'id': id});
                    },
            ),
          ],
          const SizedBox(height: 13),
          const Text(
            'Monster voice',
            style: TextStyle(fontSize: 13, color: Colors.white),
          ),
          copy(
            'Deep resonance, gentle growl, and a short echo. All processed on your phone.',
          ),
          Slider(
            activeColor: const Color(0xFF9F40FA),
            inactiveColor: const Color(0xFF1D4D84),
            value: (state['monsterStrength'] as num? ?? .55).toDouble().clamp(
              0,
              1,
            ),
            divisions: 10,
            label:
                '${((state['monsterStrength'] as num? ?? .55) * 100).round()}%',
            onChanged: busy
                ? null
                : (v) => act('setMonsterStrength', {'strength': v}),
          ),
          copy(
            'Set to 0 to remove growl and echo. Voice character still controls pitch. Your uploaded greeting plays as recorded.',
          ),
          Wrap(
            spacing: 8,
            children: [
              button(
                'Preview voice',
                'previewVoice',
                icon: Icons.play_arrow,
                disabled: mode == 'off',
              ),
              button(
                'Android offline voice settings',
                'speechSettings',
                icon: Icons.settings_voice,
              ),
            ],
          ),
        ]),
        section('Offline conversation AI', Icons.psychology_outlined, [
          Builder(
            builder: (context) {
              final ai = state['offlineAi'] is Map
                  ? Map<String, dynamic>.from(state['offlineAi'] as Map)
                  : <String, dynamic>{};
              final status = ai['status'] as String? ?? 'notInstalled';
              final installed = ai['installed'] == true;
              final downloading =
                  status == 'pending' ||
                  status == 'downloading' ||
                  status == 'checking';
              final downloaded = (ai['downloaded'] as num? ?? 0).toDouble();
              final total = (ai['total'] as num? ?? 429000000).toDouble();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  copy(
                    installed
                        ? 'Ready on this phone. Ghost can answer casual questions privately without an API or server.'
                        : 'Optional 429 MB download for private, Jarvis-style conversation. It is not included in the app download and phone commands continue to work without it.',
                  ),
                  if (downloading) ...[
                    const SizedBox(height: 10),
                    LinearProgressIndicator(
                      value: status == 'checking'
                          ? null
                          : (total > 0 ? downloaded / total : null),
                    ),
                    const SizedBox(height: 6),
                    copy(
                      status == 'checking'
                          ? 'Checking model integrity…'
                          : 'Downloading on Wi-Fi… ${(downloaded / 1000000).round()} MB',
                    ),
                  ],
                  if (installed)
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      dense: true,
                      title: const Text(
                        'Use offline AI',
                        style: TextStyle(fontSize: 13),
                      ),
                      subtitle: copy(
                        'Phone actions still use Ghost’s safe command engine.',
                      ),
                      value: ai['enabled'] != false,
                      onChanged: busy
                          ? null
                          : (v) => act('setOfflineAiEnabled', {'enabled': v}),
                    ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      if (!installed && !downloading)
                        button(
                          'Download on Wi-Fi',
                          'downloadOfflineAi',
                          icon: Icons.download_outlined,
                          filled: true,
                        ),
                      if (downloading)
                        button(
                          'Cancel download',
                          'cancelOfflineAiDownload',
                          icon: Icons.close,
                        ),
                      if (installed)
                        button(
                          'Remove model',
                          'removeOfflineAi',
                          icon: Icons.delete_outline,
                        ),
                      button(
                        'Refresh status',
                        'refreshOfflineAi',
                        icon: Icons.refresh,
                      ),
                    ],
                  ),
                  if (status == 'failed')
                    copy(
                      'The download failed. Check Wi-Fi and available storage, then try again.',
                    ),
                  const SizedBox(height: 6),
                  copy(
                    'Uses Qwen3 0.6B Q4_0 through llama.cpp. Conversations stay on this phone. Allow roughly 600 MB free during setup.',
                  ),
                ],
              );
            },
          ),
        ]),
        section('WhatsApp contacts', Icons.chat_outlined, [
          copy(
            'Say “message Harsh on WhatsApp with hi”. Ghost finds phone contacts and asks which full name when more than one matches.',
          ),
          Align(
            alignment: Alignment.centerRight,
            child: button(
              state['contactsAllowed'] == true
                  ? 'Contacts connected'
                  : 'Allow phone contacts',
              'requestContacts',
              icon: Icons.contacts_outlined,
            ),
          ),
          if (state['autoSendAvailable'] == true)
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: const Text(
                'Send through WhatsApp',
                style: TextStyle(fontSize: 13),
              ),
              subtitle: copy(
                'Optional. After your command, Ghost checks the selected chat and message, then taps Send once. Otherwise it prepares a draft for you.',
              ),
              value: state['autoSend'] == true,
              onChanged: busy
                  ? null
                  : (v) => act('setAutoSend', {'enabled': v}),
            ),
          if (state['autoSendAvailable'] != true)
            copy(
              'I’ll open the right chat with your message ready. Check it and press Send in WhatsApp.',
            ),
          if (state['autoSendAvailable'] == true &&
              state['autoSend'] == true &&
              state['whatsappAccess'] != true) ...[
            copy(
              'Enable Ghost WhatsApp Send in Android Accessibility to finish setup.',
            ),
            button('Open Accessibility settings', 'accessibilitySettings'),
          ],
        ], badgeColor: const Color(0xFF008C58)),
        section('Calls, media & notifications', Icons.headset_mic_outlined, [
          copy(
            'Media commands such as “pause music”, “next track”, and “volume down” work while locked.',
          ),
          const SizedBox(height: 8),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.call_outlined, color: Color(0xFF48DEFF)),
            title: const Text(
              'Call saved contacts',
              style: TextStyle(fontSize: 13),
            ),
            subtitle: copy(
              state['phoneCallAllowed'] == true
                  ? 'Ready. Every outgoing call requires review while the phone is unlocked.'
                  : 'Contacts and Phone permission are required. Ghost never calls an uncertain contact.',
            ),
            trailing: state['phoneCallAllowed'] == true
                ? const Icon(Icons.check_circle, color: Color(0xFF00DE9B))
                : null,
            onTap: busy || state['phoneCallAllowed'] == true
                ? null
                : () => act('requestCallAccess'),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            dense: true,
            title: const Text(
              'Read notifications by voice',
              style: TextStyle(fontSize: 13),
            ),
            subtitle: copy(
              'Opt-in. Ghost reads up to five recent notifications only while your phone is unlocked. Say “reply to Harsh with I’ll call later”; Ghost reads it back and waits for yes or no.',
            ),
            value: state['notificationReading'] == true,
            onChanged: busy
                ? null
                : (v) => act('setNotificationReading', {'enabled': v}),
          ),
          if (state['notificationReading'] == true &&
              state['notificationAccess'] != true)
            button(
              'Enable Android notification access',
              'notificationAccessSettings',
              icon: Icons.notifications_active_outlined,
              filled: true,
            ),
          if (state['notificationAccess'] == true)
            copy(
              'Notification access connected · ${state['notificationCount'] ?? 0} recent items held in memory.',
            ),
          copy(
            'Notification contents are kept in memory only and are removed when Android dismisses them or Ghost stops.',
          ),
        ]),
        section('Routines & driving', Icons.route_outlined, [
          copy(
            'Create routines by voice, for example: “create a routine called commute with start driving mode then play music”. Routines are stored locally and only allow app opening, media controls, and driving mode.',
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            dense: true,
            title: const Text('Driving mode', style: TextStyle(fontSize: 13)),
            subtitle: copy('Keeps spoken replies brief and can use a connected Bluetooth headset for listening.'),
            value: state['drivingMode'] == true,
            onChanged: busy ? null : (v) => act('setDrivingMode', {'enabled': v}),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            dense: true,
            title: const Text('Prefer Bluetooth audio', style: TextStyle(fontSize: 13)),
            subtitle: copy(_bluetoothStatus(state)),
            value: state['preferBluetooth'] != false,
            onChanged: busy ? null : (v) => act('setPreferBluetooth', {'enabled': v}),
          ),
          const SizedBox(height: 6),
          Builder(builder: (context) {
            final routines = state['routines'] as List? ?? const [];
            if (routines.isEmpty) return copy('No routines saved yet.');
            return Column(children: [
              for (final raw in routines)
                Builder(builder: (context) {
                  final routine = Map<String, dynamic>.from(raw as Map);
                  final steps = (routine['steps'] as List? ?? const []).join('  •  ');
                  return ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.bolt, color: Color(0xFF48DEFF)),
                    title: Text(routine['name'] as String? ?? '', style: const TextStyle(fontSize: 13)),
                    subtitle: Text(steps, style: const TextStyle(fontSize: 11, color: muted)),
                    onTap: busy ? null : () => act('runRoutine', {'name': routine['name']}),
                    trailing: IconButton(
                      tooltip: 'Delete routine',
                      icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                      onPressed: busy ? null : () => act('deleteRoutine', {'name': routine['name']}),
                    ),
                  );
                }),
            ]);
          }),
        ]),
        section('Personalization', Icons.auto_awesome, [
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Assistant name', style: TextStyle(fontSize: 13)),
            subtitle: copy(state['assistantName'] as String? ?? 'Ghost'),
            trailing: const Icon(Icons.edit_outlined, color: muted),
            onTap: busy ? null : rename,
          ),
          copy(
            'Changes the greeting and offline wake phrase. Short English names work best with the bundled speech model; “Hey Ghost” remains available as a fallback.',
          ),
        ]),
        section('Reminders', Icons.notifications_active_outlined, [
          Builder(
            builder: (context) {
              final reminders = state['reminders'] as List? ?? const [];
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  copy(
                    'Say “remind me to call Harsh at 6 PM tomorrow.” Reminders are stored on this phone and restored after reboot.',
                  ),
                  const SizedBox(height: 8),
                  if (reminders.isEmpty)
                    copy('No scheduled reminders.')
                  else
                    for (final raw in reminders)
                      Builder(
                        builder: (context) {
                          final reminder = Map<String, dynamic>.from(
                            raw as Map,
                          );
                          return ListTile(
                            dense: true,
                            contentPadding: EdgeInsets.zero,
                            leading: const Icon(
                              Icons.schedule,
                              color: Color(0xFF48DEFF),
                            ),
                            title: Text(
                              reminder['text'] as String? ?? '',
                              style: const TextStyle(fontSize: 13),
                            ),
                            subtitle: Text(
                              reminderTime(reminder['triggerAt'] as num),
                              style: const TextStyle(
                                fontSize: 11,
                                color: muted,
                              ),
                            ),
                            trailing: IconButton(
                              tooltip: 'Delete reminder',
                              icon: const Icon(
                                Icons.delete_outline,
                                color: Colors.redAccent,
                              ),
                              onPressed: busy
                                  ? null
                                  : () => act('deleteReminder', {
                                      'id': reminder['id'],
                                    }),
                            ),
                          );
                        },
                      ),
                  const SizedBox(height: 6),
                  button(
                    'Allow exact reminder timing',
                    'exactAlarmSettings',
                    icon: Icons.alarm_on_outlined,
                  ),
                  copy(
                    'Without exact-alarm access, Android may delay a reminder slightly to save battery.',
                  ),
                ],
              );
            },
          ),
        ]),
        section('Emergency protection', Icons.emergency_share_outlined, [
          Builder(
            builder: (context) {
              final emergency = state['emergency'] is Map
                  ? Map<String, dynamic>.from(state['emergency'] as Map)
                  : <String, dynamic>{};
              final configured = emergency['configured'] == true;
              final ready = emergency['permissionsReady'] == true;
              final active = emergency['active'] == true;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  copy(
                    configured
                        ? 'Trusted contact ${emergency['phoneMasked']}. The local phrase sends SMS location updates. The separate Ghost Link phrase sends one emergency SMS from the connected phone.'
                        : 'Set a private emergency phrase, stop phrase, and trusted personal contact. The phrase never appears in history.',
                  ),
                  const SizedBox(height: 10),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Icon(
                      configured
                          ? Icons.verified_user_outlined
                          : Icons.add_moderator_outlined,
                      color: ghostCyan,
                    ),
                    title: Text(
                      configured
                          ? 'Replace secure setup'
                          : 'Set up emergency protection',
                      style: const TextStyle(fontSize: 13),
                    ),
                    subtitle: copy(
                      'Saving new details disables protection until permissions are checked again.',
                    ),
                    onTap: busy ? null : configureEmergency,
                  ),
                  if (configured && !ready) ...[
                    copy(
                      'Android must allow SMS, precise location, and location access all the time. Tap below, approve the prompts, then choose “Allow all the time” in app permissions.',
                    ),
                    const SizedBox(height: 8),
                    button(
                      'Grant emergency permissions',
                      'requestEmergencyPermissions',
                      icon: Icons.security_outlined,
                      filled: true,
                    ),
                  ],
                  if (configured && ready)
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text(
                        'Emergency phrase active',
                        style: TextStyle(fontSize: 13),
                      ),
                      subtitle: copy(
                        'Works while Ghost listening is active, including on the lock screen.',
                      ),
                      value: emergency['enabled'] == true,
                      onChanged: busy
                          ? null
                          : (value) =>
                                act('setEmergencyEnabled', {'enabled': value}),
                    ),
                  if (active)
                    button(
                      'Stop location sharing',
                      'stopEmergency',
                      icon: Icons.stop_circle_outlined,
                    ),
                  if (configured)
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: busy ? null : () => act('clearEmergency'),
                        child: const Text('Remove emergency setup'),
                      ),
                    ),
                  copy(
                    'Linked-phone SMS works while that phone is locked when receiving and SMS permission are enabled. If it is unlocked with the Ghost Link app or bubble visible, Ghost also opens an optional WhatsApp draft and never presses Send.',
                  ),
                ],
              );
            },
          ),
        ], badgeColor: const Color(0xFF9B173D)),
        section('Listening & privacy', Icons.shield_outlined, [
          copy(
            'Wake phrase: Hey ${state['assistantName'] ?? 'Ghost'}  |  Language: English\nMicrophone audio: processed locally, never recorded\nSpoken replies: temporary private audio, removed after playback\nActivity: memory only; cleared when the process ends\nVoice preferences: saved on this phone\nContacts: looked up locally, never uploaded',
          ),
          const SizedBox(height: 7),
          copy(
            'If OriginOS stops listening, review Ghost’s background battery settings. After reboot or force-stop, open Ghost and enable listening again.',
          ),
          Align(
            alignment: Alignment.centerRight,
            child: button(
              'Android battery settings',
              'batterySettings',
              icon: Icons.battery_charging_full,
            ),
          ),
        ]),
      ],
    );
  }
}
