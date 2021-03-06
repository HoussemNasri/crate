.. highlight:: psql
.. _ref-restore-snapshot:

====================
``RESTORE SNAPSHOT``
====================

Restore a snapshot into the cluster.

.. rubric:: Table of contents

.. contents::
   :local:

Synopsis
========

::

    RESTORE SNAPSHOT repository_name.snapshot_name
    { TABLE ( table_ident [ PARTITION (partition_column = value [ , ... ])] [, ...] ) | ALL }
    [ WITH (restore_parameter [= value], [, ...]) ]

Description
===========

Restore one or more tables or partitions from an existing snapshot into the
cluster. The snapshot must be given as fully qualified reference with
``repository_name`` and ``snapshot_name``.

It is possible to restore all tables contained in the snapshot using the
``ALL`` keyword. Single tables and/or partitions can be selected for restoring
by giving them as ``table_ident`` or partition reference given the
``partition_column`` values.

Tables that are to be restored must not exist yet.

To cancel a restore operation simply drop the tables that are being restored.

Parameters
==========

:repository_name:
  The name of the repository of the snapshot to restore as ident.

:snapshot_name:
  The name of the snapshot as ident.

:table_ident:
  The name (optionally schema-qualified) of an existing table that is to be
  restored from the snapshot.

:partition_column:
  Column name by which the table is partitioned.

Clauses
=======

``WITH``
--------

::

    [ WITH (restore_parameter [= value], [, ...]) ]

The following configuration parameters can be used to modify how the snapshot
is restored to the cluster:

:ignore_unavailable:
  (Default ``false``) Per default the restore command fails if a table
  is given that does not exist in the snapshot. If set to ``true`` those
  missing tables are ignored.

:wait_for_completion:
  (Default: ``false``) By default the request returns once the restore
  operation started. If set to ``true`` the request returns after all
  selected tables from the snapshot are restored or an error occurred.
  In order to monitor the restore operation the * :ref:`sys.shards
  <sys-shards>` table can be queried.
