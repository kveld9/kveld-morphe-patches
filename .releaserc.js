const ignoredScopes = [
  'harness',
  'ci',
  'test',
  'tooling',
  'scripts',
  'docs',
  'governance',
  'agents',
  'readme',
  'github',
  'release',
  'repo',
  'security',
  'hygiene',
  'git',
  'changelog',
];

const isIgnoredScope = (scope) => {
  if (typeof scope !== 'string') {
    return false;
  }
  return ignoredScopes.includes(scope.trim().toLowerCase());
};

const filterGroupCommits = (group) => ({
  ...group,
  commits: (group.commits || []).filter((commit) => !isIgnoredScope(commit.scope)),
});

const filterGroupNotes = (group) => ({
  ...group,
  notes: (group.notes || []).filter((note) => !isIgnoredScope(note.commit?.scope || note.scope)),
});

const finalizeContext = (context) => {
  if (Array.isArray(context.commitGroups)) {
    context.commitGroups = context.commitGroups
      .map(filterGroupCommits)
      .filter((group) => group.commits.length > 0);
  }
  if (Array.isArray(context.noteGroups)) {
    context.noteGroups = context.noteGroups
      .map(filterGroupNotes)
      .filter((group) => group.notes.length > 0);
  }
  if (Array.isArray(context.commits)) {
    context.commits = context.commits.filter((commit) => !isIgnoredScope(commit.scope));
  }
  return context;
};

const ignoredScopeRules = ignoredScopes.flatMap((scope) => [
  { scope, release: false },
  { scope: scope.charAt(0).toUpperCase() + scope.slice(1), release: false },
  { scope: scope.toUpperCase(), release: false },
]);

module.exports = {
  branches: ['main'],
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        releaseRules: [
          { type: 'fix', release: 'patch' },
          { type: 'feat', release: 'minor' },
          { type: 'bump', release: 'patch' },
          { type: 'perf', release: 'patch' },
          { type: 'refactor', release: false },
          { type: 'build', scope: 'Needs bump', release: 'patch' },
          ...ignoredScopeRules,
        ],
      },
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
        presetConfig: {
          types: [
            { type: 'fix', section: 'Bug Fixes', hidden: false },
            { type: 'feat', section: 'New Features', hidden: false },
            { type: 'bump', section: 'Updated App Support', hidden: false },
            { type: 'perf', section: 'Improvements', hidden: false },
            { type: 'refactor', hidden: true },
            { type: 'build', hidden: true },
          ],
        },
        writerOpts: {
          finalizeContext,
        },
      },
    ],
    [
      '@MorpheApp/changelog',
      {
        releaseJson: {
          path: 'patches-bundle.json',
          downloadUrlTemplate:
            'https://github.com/${owner}/${repo}/releases/download/v${version}/patches-${version}.mpp',
          signatureUrlTemplate: '',
        },
      },
    ],
    'gradle-semantic-release-plugin',
    [
      '@semantic-release/exec',
      {
        prepareCmd: [
          './gradlew generatePatchesList',
          'jq \'.version="${nextRelease.version}"\' patches-list.json > patches-list.json.tmp && mv patches-list.json.tmp patches-list.json',
          'python3 .github/scripts/generate_patches_readme.py $GITHUB_REPOSITORY $GITHUB_REF_NAME patches-list.json README.md',
          'python3 .github/scripts/sync_contributors.py --repo $GITHUB_REPOSITORY',
        ].join(' && '),
      },
    ],
    [
      '@semantic-release/git',
      {
        assets: [
          '.github/contributors.json',
          'CHANGELOG.md',
          'gradle.properties',
          'patches-bundle.json',
          'patches-list.json',
          'README.md',
        ],
        message:
          'chore: Release v${nextRelease.version} [skip ci]\n\n${nextRelease.notes}',
      },
    ],
    [
      '@semantic-release/github',
      {
        assets: [
          {
            path: 'patches/build/libs/patches-!(*sources*|*javadoc*).mpp?(.asc)',
          },
        ],
        successComment: false,
      },
    ],
  ],
};
