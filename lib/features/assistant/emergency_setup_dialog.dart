import 'package:flutter/material.dart';

import 'ghost_home.dart';
import 'futuristic_shell.dart';

class EmergencySetup {
  const EmergencySetup(this.phrase, this.remotePhrase, this.stopPhrase, this.phone, this.message);
  final String phrase, remotePhrase, stopPhrase, phone, message;
}

class EmergencySetupDialog extends StatefulWidget {
  const EmergencySetupDialog({super.key});
  @override
  State<EmergencySetupDialog> createState() => _EmergencySetupDialogState();
}

class _EmergencySetupDialogState extends State<EmergencySetupDialog> {
  final phrase = TextEditingController();
  final remotePhrase = TextEditingController();
  final stopPhrase = TextEditingController(text: 'cancel emergency');
  final phone = TextEditingController();
  final message = TextEditingController(text: 'I may need help. Please check my location.');
  bool hidden = true;

  @override
  void dispose() {
    phrase.dispose(); remotePhrase.dispose(); stopPhrase.dispose(); phone.dispose(); message.dispose();
    super.dispose();
  }

  void save() {
    if (phrase.text.trim().length < 3 || remotePhrase.text.trim().length < 3 || stopPhrase.text.trim().length < 3 || phone.text.replaceAll(RegExp(r'\D'), '').length < 7) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enter all three phrases and a valid trusted contact number.')));
      return;
    }
    Navigator.pop(context, EmergencySetup(phrase.text, remotePhrase.text, stopPhrase.text, phone.text, message.text));
  }

  @override
  Widget build(BuildContext context) => Dialog(
    backgroundColor: Colors.transparent,
    insetPadding: const EdgeInsets.all(20),
    child: Container(
      constraints: const BoxConstraints(maxWidth: 520),
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: const LinearGradient(colors: [Color(0xFF071A34), Color(0xFF100A30)]),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: ghostCyan),
        boxShadow: [BoxShadow(color: ghostPurple.withValues(alpha: .45), blurRadius: 38)],
      ),
      child: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        const Icon(Icons.emergency_share_outlined, color: ghostCyan, size: 50),
        const SizedBox(height: 12),
        const Text('Emergency protection', textAlign: TextAlign.center, style: TextStyle(fontSize: 25, fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        const Text('The local phrase starts location SMS updates from this phone. A separate Ghost Link phrase sends an emergency SMS from the connected phone.', textAlign: TextAlign.center, style: TextStyle(color: Color(0xFFADBEDC), height: 1.4)),
        const SizedBox(height: 18),
        TextField(controller: phrase, obscureText: hidden, enableSuggestions: false, autocorrect: false, decoration: InputDecoration(labelText: 'Private emergency phrase', suffixIcon: IconButton(onPressed: () => setState(() => hidden = !hidden), icon: Icon(hidden ? Icons.visibility_outlined : Icons.visibility_off_outlined)))),
        const SizedBox(height: 12),
        TextField(controller: remotePhrase, obscureText: hidden, enableSuggestions: false, autocorrect: false, decoration: const InputDecoration(labelText: 'Ghost Link emergency phrase')),
        const SizedBox(height: 12),
        TextField(controller: stopPhrase, obscureText: hidden, enableSuggestions: false, autocorrect: false, decoration: const InputDecoration(labelText: 'Private stop phrase')),
        const SizedBox(height: 12),
        TextField(controller: phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Trusted contact phone number')),
        const SizedBox(height: 12),
        TextField(controller: message, maxLength: 160, maxLines: 2, decoration: const InputDecoration(labelText: 'Alert message')),
        const Text('SMS carrier charges may apply. Do not enter 112 or another public emergency number. Phrases are stored as one-way verifiers and cannot be recovered.', style: TextStyle(color: Color(0xFFADBEDC), fontSize: 11, height: 1.4)),
        const SizedBox(height: 18),
        Row(children: [
          Expanded(child: OutlinedButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel'))),
          const SizedBox(width: 10),
          Expanded(child: Container(decoration: BoxDecoration(gradient: neonGradient, borderRadius: BorderRadius.circular(14)), child: TextButton(onPressed: save, child: const Text('Save securely', style: TextStyle(color: Colors.white))))),
        ]),
      ])),
    ),
  );
}
