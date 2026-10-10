import 'package:flutter/material.dart';

class InstalledAppsSheet extends StatefulWidget {
  const InstalledAppsSheet({super.key, required this.apps});
  final List<Map<String, dynamic>> apps;

  @override
  State<InstalledAppsSheet> createState() => _InstalledAppsSheetState();
}

class _InstalledAppsSheetState extends State<InstalledAppsSheet> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final matches = widget.apps
        .where(
          (app) =>
              '${app['name']} ${app['package']}'.toLowerCase().contains(_query),
        )
        .toList();
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          20,
          20,
          MediaQuery.viewInsetsOf(context).bottom + 12,
        ),
        child: SizedBox(
          height: MediaQuery.sizeOf(context).height * .7,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Installed apps', style: TextStyle(fontSize: 24)),
              const SizedBox(height: 12),
              TextField(
                decoration: const InputDecoration(hintText: 'Search app name'),
                onChanged: (value) =>
                    setState(() => _query = value.trim().toLowerCase()),
              ),
              const SizedBox(height: 12),
              Expanded(
                child: matches.isEmpty
                    ? const Center(child: Text('No matching launchable apps.'))
                    : ListView.builder(
                        itemCount: matches.length,
                        itemBuilder: (context, index) {
                          final app = matches[index];
                          return ListTile(
                            leading: const Icon(Icons.apps),
                            title: Text(app['name'] as String),
                            subtitle: Text(app['package'] as String),
                            trailing: const Icon(Icons.open_in_new),
                            onTap: () => Navigator.pop(
                              context,
                              app['package'] as String,
                            ),
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
